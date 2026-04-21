<template>
    <v-card class="my-6" :elevation="10">
        <v-card-title>Übersicht per Land
            <Suspense>
                <AppCardToggle v-model="hideContent" id="Übersicht-per-Land" />
            </Suspense>
        </v-card-title>
        <v-card-text v-show="!hideContent">
            <v-row class="mx-3 mt-1 mb-1">
                <div style="width: 170px">
                    <v-text-field v-model="filter" label="Suchen..." prepend-inner-icon="mdi-magnify" variant="outlined"
                        density="compact" hide-details />
                </div>
            </v-row>

            <v-data-table-virtual :headers="headers" :items="filteredEntries" height="500px" density="compact"
                :sort-by="[{ key: 'year-0', order: 'desc' }]" fixed-header fixed-footer striped="even"
                no-data-text="keine Daten vorhanden">
                <template #item.country="{ item }">
                    <div class="align-center d-flex">
                        <span style="width:35px; display: inline-block">
                            <img v-if="getCountryInfo(item.country)" :src="getFlag(getCountryInfo(item.country)!)"
                                :alt="getCountryInfo(item.country)!.name" class="country-flag mt-1">
                        </span>
                        {{ item.country }}
                    </div>
                </template>
                <template #item.year-0="{ item }">
                    {{ formatCurrency(item['year-0']) }}
                </template>
                <template #item.year-1="{ item }">
                    {{ formatCurrency(item['year-1']) }}
                </template>
                <template #item.year-2="{ item }">
                    {{ formatCurrency(item['year-2']) }}
                </template>
                <template #item.year-3="{ item }">
                    {{ formatCurrency(item['year-3']) }}
                </template>
                <template #item.total="{ item }">
                    {{ formatCurrency(item.total) }}
                </template>
            </v-data-table-virtual>
        </v-card-text>
    </v-card>
</template>
<script
    setup
    lang="ts"
>
    import { computed, ref } from 'vue';
    import { Dividend, QSt } from '../models';
    import { formatCurrency, getFlag } from '../utils';
    import { db } from '../db';
    import AppCardToggle from './incl/AppCardToggle.vue';

    type DataType = {
        country: string;
        total: number;
        "year-0": number;
        "year-1": number;
        "year-2": number;
        "year-3": number;
    }

    const year = new Date().getFullYear();
    const headers = ref([
        { title: 'Land', key: 'country', align: 'start' as const },
        { title: `Summe`, key: 'total', align: 'end' as const },
        { title: `${year}`, key: 'year-0', align: 'end' as const },
        { title: `${year - 1}`, key: 'year-1', align: 'end' as const },
        { title: `${year - 2}`, key: 'year-2', align: 'end' as const },
        { title: `${year - 3}`, key: 'year-3', align: 'end' as const },
    ])


    const entries = defineModel<Dividend[]>({ default: () => [] });

    const filter = ref('');

    const hideContent = ref(false);

    const qsts = await db.getQstsAll();
    const getCountryInfo = (isin: string): QSt | null => {
        return qsts.find((qst: QSt) => qst.iso === isin.slice(0, 2).toUpperCase()) || null;
    };

    const dataEntries = computed(() => {
        const countries = new Set(entries.value.map(entry => entry.isin.slice(0, 2)));
        return Array.from(countries).map(country => {
            // const countryInfo = getCountryInfo(entry.isin);
            const countryEntries = entries.value.filter(entry => entry.isin.slice(0, 2) === country);
            return {
                country: country,
                total: countryEntries.reduce((sum, entry) => sum + entry.amount, 0),
                "year-0": countryEntries.filter(entry => entry.date.getFullYear() === year).reduce((sum, entry) => sum + entry.amount, 0),
                "year-1": countryEntries.filter(entry => entry.date.getFullYear() === year - 1).reduce((sum, entry) => sum + entry.amount, 0),
                "year-2": countryEntries.filter(entry => entry.date.getFullYear() === year - 2).reduce((sum, entry) => sum + entry.amount, 0),
                "year-3": countryEntries.filter(entry => entry.date.getFullYear() === year - 3).reduce((sum, entry) => sum + entry.amount, 0),
            };
        });
    });

    const filteredEntries = computed(() => {
        return filterTable(dataEntries.value);
    });

    function filterTable(result: DataType[]): DataType[] {
        if (filter.value) {
            const searchTerm = filter.value.toLowerCase();
            result = result.filter(entry => entry.country.toLowerCase().includes(searchTerm));
        }
        return result;
    }
</script>
