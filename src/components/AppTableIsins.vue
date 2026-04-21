<template>
    <AppEditIsinInfoDialog v-model="editIsin" @isin-updated="updateIsinData" />
    <AppQueryIsinInfoDialog v-model="showQueryIsinInfo" @isin-updated="updateIsinData" />

    <v-card class="my-6" :elevation="10">
        <v-card-title>Übersicht per ISIN
            <Suspense>
                <AppCardToggle v-model="hideContent" id="Übersicht-per-ISIN" />
            </Suspense>
        </v-card-title>
        <v-card-text v-show="!hideContent">
            <v-row class="mx-3 mt-1 mb-1">
                <div style="width: 170px">
                    <v-text-field v-model="filter" label="Suchen..." prepend-inner-icon="mdi-magnify" variant="outlined"
                        density="compact" hide-details />
                </div>
                <div style="width: 140px" class="ml-5">
                    <v-checkbox :model-value="showOnlyEtf" label="Nur ETF/ETP..." density="compact" hide-details
                        :indeterminate="showOnlyEtf === null" @click.prevent.stop="toggleShowOnlyEtf()" />
                </div>
                <div style="width: 100px" class="ml-5">
                    <v-checkbox v-model="showOnlyUnnamed" label="Nur o.N." density="compact" hide-details />
                </div>
            </v-row>

            <v-data-table-virtual :headers="headers" :items="filteredEntries" height="500px" density="compact"
                class="ma-0" :sort-by="[{ key: 'year-0', order: 'desc' }]" fixed-header fixed-footer striped="even"
                no-data-text="keine Daten vorhanden">
                <template #item.isin="{ item }">
                    <table border="0" style="width: 100%; table-layout: fixed; border-collapse: collapse;">
                        <tbody>
                            <tr>
                                <td style="width: clamp(30px, 30px, 30px);">
                                    <v-menu width="150px" location-strategy="connected">
                                        <template #activator="{ props }">
                                            <v-icon v-bind="props">mdi-dots-vertical</v-icon>
                                        </template>
                                        <v-card elevation="20">
                                            <v-card-title />
                                            <v-card-subtitle class="text-center">
                                                {{ item.isin }}
                                            </v-card-subtitle>
                                            <v-card-text class="pa-2 text-center">
                                                <v-btn icon density="compact" class="update-data-icon mx-2" color="blue"
                                                    variant="tonal" @click="showQueryIsinInfo = item.isin">
                                                    <v-icon icon="mdi-file-document-refresh-outline" />
                                                    <v-tooltip activator="parent" open-delay="200" location="top">
                                                        ISIN-Informationen abfragen
                                                    </v-tooltip>
                                                </v-btn>
                                                <v-btn icon density="compact" class="update-data-icon mx-2"
                                                    color="green" variant="tonal" @click="editIsinInfo(item.isin)">
                                                    <v-icon icon="mdi-pencil" />
                                                    <v-tooltip activator="parent" open-delay="200" location="top">
                                                        ISIN-Informationen editieren...
                                                    </v-tooltip>
                                                </v-btn>
                                                <v-btn icon="mdi-file-pdf-box" color="purple"
                                                    class="update-data-icon mx-2" @click="exportToPDF(item)"
                                                    density="compact" variant="tonal" title="Als PDF exportieren" />
                                            </v-card-text>
                                        </v-card>
                                    </v-menu>
                                </td>
                                <td style="width: clamp(45px, 45px, 45px);">
                                    <img v-if="getCountryInfo(item.isin)" :src="getFlag(getCountryInfo(item.isin)!)"
                                        :alt="getCountryInfo(item.isin)!.name" class="country-flag mt-1">
                                </td>
                                <td style="width: clamp(125px, 125px, 125px);">
                                    {{ item.isin }}
                                </td>
                                <td v-if="!isinData[item.isin]"
                                    style="width: clamp(35px, 35px, 35px); text-align: center"></td>
                                <td v-else style="width: clamp(35px, 35px, 35px); text-align: center">
                                    <template v-if="isEtf(isinData[item.isin].type)">
                                        <input type="checkbox" checked readonly />
                                        <v-tooltip activator="parent" open-delay="100" location="top">
                                            {{ isinData[item.isin].type }}
                                        </v-tooltip>
                                    </template>
                                    <template v-else>
                                        <input type="checkbox" readonly />
                                        <v-tooltip activator="parent" open-delay="100" location="top">
                                            {{ isinData[item.isin].type }}
                                        </v-tooltip>
                                    </template>
                                </td>

                                <td
                                    style="width: clamp(80px, 100%, 100%); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; display: inline-block;">
                                    <span v-if="isinData[item.isin]">
                                        {{ isinData[item.isin].name }}
                                        <v-tooltip activator="parent" open-delay="100" location="top">
                                            {{ isinData[item.isin].name }}
                                        </v-tooltip>
                                    </span>
                                </td>
                            </tr>

                        </tbody>
                    </table>
                </template>
                <template #item.year-0="{ item }">
                    {{ formatCurrency(item['year-0'], false, true) }}
                </template>
                <template #item.year-1="{ item }">
                    {{ formatCurrency(item['year-1'], false, true) }}
                </template>
                <template #item.year-2="{ item }">
                    {{ formatCurrency(item['year-2'], false, true) }}
                </template>
                <template #item.year-3="{ item }">
                    {{ formatCurrency(item['year-3'], false, true) }}
                </template>
                <template #item.total="{ item }">
                    {{ formatCurrency(item.total, false, true) }}
                </template>
            </v-data-table-virtual>
        </v-card-text>
    </v-card>
</template>
<script
    setup
    lang="ts"
>
    import { computed, onMounted, ref } from 'vue';
    import { Dividend, Isin, IsinOverview, QSt } from '../models';
    import { formatCurrency, getFlag } from '../utils';
    import { db } from '../db';
    import AppCardToggle from './incl/AppCardToggle.vue';
    import { exportToPDF } from '../pdf';
    import AppQueryIsinInfoDialog from './incl/AppQueryIsinInfoDialog.vue';
    import AppEditIsinInfoDialog from './incl/AppEditIsinInfoDialog.vue';


    type DataType = {
        isin: string;
        total: number;
        "year-0": number;
        "year-1": number;
        "year-2": number;
        "year-3": number;
    }

    const year = new Date().getFullYear();
    const headers = ref([
        { title: 'ISIN', key: 'isin', align: 'start' as const },
        { title: `Summe\u00a0(€)`, key: 'total', align: 'end' as const },
        { title: `${year - 0}\u00a0(€)`, key: 'year-0', align: 'end' as const },
        { title: `${year - 1}\u00a0(€)`, key: 'year-1', align: 'end' as const },
        { title: `${year - 2}\u00a0(€)`, key: 'year-2', align: 'end' as const },
        { title: `${year - 3}\u00a0(€)`, key: 'year-3', align: 'end' as const },
    ]);

    const entries = defineModel<Dividend[]>({ default: () => [] });

    const isinData = ref<Record<string, Isin>>({});

    const filter = ref('');
    const editIsin = ref<Isin | null>(null);

    const hideContent = ref(false);
    const showQueryIsinInfo = ref('');
    const showOnlyEtf = ref<boolean | null>(false);
    const showOnlyUnnamed = ref<boolean>(false);

    const qsts = await db.getQstsAll();

    onMounted(() => updateIsinData());

    const getCountryInfo = (isin: string): QSt | null => {
        return qsts.find((qst: QSt) => qst.iso === isin.slice(0, 2).toUpperCase()) || null;
    };


    const dataEntries = computed(() => {
        const isins = new Set(entries.value.map(entry => entry.isin));
        return Array.from(isins).map(isin => {
            // const countryInfo = getCountryInfo(entry.isin);
            const isinEntries = entries.value.filter(entry => entry.isin === isin);
            return {
                isin: isin,
                total: isinEntries.reduce((sum, entry) => sum + entry.amount, 0),
                "year-0": isinEntries.filter(entry => entry.date.getFullYear() === year).reduce((sum, entry) => sum + entry.amount, 0),
                "year-1": isinEntries.filter(entry => entry.date.getFullYear() === year - 1).reduce((sum, entry) => sum + entry.amount, 0),
                "year-2": isinEntries.filter(entry => entry.date.getFullYear() === year - 2).reduce((sum, entry) => sum + entry.amount, 0),
                "year-3": isinEntries.filter(entry => entry.date.getFullYear() === year - 3).reduce((sum, entry) => sum + entry.amount, 0),
            } as IsinOverview;
        });
    });

    const filteredEntries = computed(() => {
        return filterTable(dataEntries.value);
    });

    function updateIsinData(): void {
        db.getIsinsAll().then(data => {
            isinData.value = Object.fromEntries(data.map(entry => [entry.isin, { name: entry.name, type: entry.type, isin: entry.isin }]));
            console.debug('isin-data', isinData.value);
        });
    }

    function filterTable(result: DataType[]): DataType[] {
        if (filter.value) {
            const searchTerm = filter.value.toLowerCase();
            result = result.filter(entry => entry.isin.toLowerCase().includes(searchTerm));
        }

        if (showOnlyUnnamed.value) {
            result = result.filter(entry => !isinData.value[entry.isin]?.name);
        }

        if (showOnlyEtf.value === true) {
            result = result.filter(entry => {
                const isinType = isinData.value[entry.isin]?.type;
                return isinType && isEtf(isinType);
            });
        } else if (showOnlyEtf.value === null) {
            result = result.filter(entry => {
                const isinType = isinData.value[entry.isin]?.type;
                return !isinType || !isEtf(isinType);
            });
        }

        return result;
    }

    function editIsinInfo(isin: string) {
        editIsin.value = { isin, name: isinData.value[isin]?.name ?? '', type: isinData.value[isin]?.type ?? '' };
    }

    function isEtf(type: string): boolean {
        return !['Common Stock', 'Preference'].includes(type);
    }

    function toggleShowOnlyEtf(): void {
        if (showOnlyEtf.value === false) {
            console.debug('false => true')
            showOnlyEtf.value = true;
        } else if (showOnlyEtf.value === true) {
            console.debug('true => null')
            showOnlyEtf.value = null;
        } else if (showOnlyEtf.value === null) {
            console.debug('null => false')
            showOnlyEtf.value = false;
        }
    }

</script>
<style scoped>
    .update-data-icon {
        opacity: 0.9;
    }

    .update-data-icon:hover {
        opacity: 1;
    }
</style>
