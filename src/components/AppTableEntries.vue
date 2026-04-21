<template>
    <v-card :elevation="10">
        <v-card-title>Gespeicherte Einträge
            <Suspense>
                <AppCardToggle v-model="hideContent" id="Gespeicherte-Einträge" />
            </Suspense>
        </v-card-title>
        <v-card-text v-show="!hideContent">
            <table class="w-100 mb-5">
                <tbody>
                    <tr>
                        <td style="width: 170px">
                            <v-text-field v-model="filter" label="Suchen..." prepend-inner-icon="mdi-magnify"
                                variant="outlined" density="compact" hide-details />
                        </td>
                        <td class="text-center px-5">
                            <v-row>
                                <v-col cols="1" md="1" sm="2" class="d-flex align-center">
                                    <v-icon icon="mdi-checkbox-multiple-outline" class="cursor-pointer mr-1"
                                        @click="selectedDivs.push(...paginatedEntries.map(entry => entry.id))" />
                                    <v-icon icon="mdi-checkbox-multiple-blank-outline" class="cursor-pointer ml-1"
                                        @click="selectedDivs.splice(0)" />

                                </v-col>
                                <v-col v-if="selectedDivs.length > 0">
                                    <v-combobox label="Tags ändern" :items="props.tags" hide-details
                                        v-model="selectedTags" density="compact" multiple indeterminate closable-chips
                                        clearable @update:model-value="bulkEditTags($event, selectedDivs)">
                                        <template #append-inner>
                                            {{ selectedDivs.length }}/{{ entries.length }}
                                        </template>
                                    </v-combobox>
                                </v-col>
                                <v-col cols="4" v-if="selectedDivs.length === 0">
                                    <v-select v-model="tableFromLabel" label="Von" density="compact" hide-details
                                        @update:model-value="updateTable()" variant="solo-filled"
                                        :items="[...tableLabels]" style="zoom: 0.8" />
                                </v-col>
                                <v-col cols="4" v-if="selectedDivs.length === 0">
                                    <v-select v-model="tableUntilLabel" label="Bis" density="compact" hide-details
                                        @update:model-value="updateTable()" variant="solo-filled"
                                        :items="[...tableLabels]" style="zoom: 0.8" />
                                </v-col>
                            </v-row>
                        </td>
                        <td style=" width: 100px;">
                            <v-select v-model="filterByTag" :items="tags" label="Tags-Filter" variant="outlined"
                                density="compact" hide-details />
                        </td>
                        <td style=" width: 100px;">
                            <v-select v-model="rows" :items="[10, 15, 20, 30, 40, 50, 75, 100]" label="pro Seite"
                                variant="outlined" density="compact" hide-details />
                        </td>
                    </tr>
                </tbody>
            </table>

            <v-table density="comfortable">
                <thead>
                    <tr>
                        <th @click="sort('isin')" class="cursor-pointer">
                            ISIN <v-icon class="cursor-pointer">{{ sortIcon('isin') }}</v-icon>
                        </th>
                        <th @click="sort('amount')" class="cursor-pointer text-center" style="width: 180px">
                            Betrag (€) <v-icon class="cursor-pointer">{{ sortIcon('amount') }}</v-icon>
                        </th>
                        <th @click="sort('date')" class="cursor-pointer">
                            Zahltag <v-icon class="cursor-pointer">{{ sortIcon('date') }}</v-icon>
                        </th>
                        <th>Tags</th>
                        <th style="width: 200px;">
                            Steuerdetails
                            <!-- br>
                            <v-row dense no-gutters>
                                <v-col class="font-8">Quellensteuer</v-col>
                                <v-col class="font-8">Anrechenbar</v-col>
                                <v-col class="font-8">Erstattbar</v-col>
                            </v-row -->
                        </th>
                        <th>Aktionen</th>
                    </tr>
                </thead>
                <tbody v-if="paginatedEntries.length > 0">
                    <tr v-for="entry in paginatedEntries" :key="entry.id">
                        <td>
                            <v-hover>
                                <template v-slot:default="{ isHovering, props }">
                                    <table v-bind="props">
                                        <tbody>
                                            <tr>
                                                <td class="align-center text-left" style="width: 40px;">
                                                    <img v-if="getCountryInfo(entry.isin)"
                                                        :src="getFlag(getCountryInfo(entry.isin)!)"
                                                        :alt="getCountryInfo(entry.isin)!.name"
                                                        class="country-flag mt-1">
                                                    <div v-else class="country-flag font-8 text-center mx-auto">
                                                        {{ entry.isin.toUpperCase().slice(0, 2) }}
                                                    </div>
                                                </td>
                                                <td class="text-left" style="width: 120px;">
                                                    {{ entry?.isin }}
                                                </td>
                                                <td class="text-left align-center justify-center bg-red"
                                                    style="width: 40px;">
                                                    <v-checkbox v-if="isHovering || selectedDivs.includes(entry.id)"
                                                        class="mx-auto" multiple :value="entry.id"
                                                        v-model="selectedDivs" hide-details density="compact" />
                                                </td>
                                            </tr>
                                        </tbody>
                                    </table>
                                </template>
                            </v-hover>
                        </td>
                        <td class="text-center">
                            <div class="text-right" style="width: 100px">{{ formatCurrency(entry.amount) }}</div>
                        </td>
                        <td>{{ formatDate(entry.date) }}</td>
                        <td>
                            <div class="d-flex flex-wrap gap-1">
                                <v-chip v-for="(tag, index) in entry.tags" :key="index" size="small">
                                    {{ tag }}
                                </v-chip>
                            </div>
                        </td>
                        <td>
                            <!--
                            <v-row v-if="getCountryInfo(entry.isin)" class="font-8" dense no-gutters>
                                <v-col>{{ formatCurrency(calculateWithholdingTax(entry)) }}</v-col>
                                <v-col>{{ formatCurrency(calculateCreditableTax(entry)) }}</v-col>
                                <v-col>{{ formatCurrency(calculateRefundableTax(entry)) }}</v-col>
                            </v-row>
                            -->

                            <div v-if="getCountryInfo(entry.isin)" class="font-9" style="line-height: 10px;">
                                <div class="space-between pt-1">
                                    <span class="tax-label">Quellensteuer:</span>
                                    <span>{{ formatCurrency(calculateWithholdingTax(entry, getCountryInfo(entry.isin)!))
                                    }}</span>
                                </div>
                                <div class="space-between">
                                    <span class="tax-label">Anrechenbar:</span>
                                    <span>{{ formatCurrency(calculateCreditableTax(entry, getCountryInfo(entry.isin)!))
                                    }}</span>
                                </div>
                                <div class="space-between">
                                    <span class="tax-label">Erstattbar:</span>
                                    <span>{{ formatCurrency(calculateRefundableTax(entry, getCountryInfo(entry.isin)!))
                                    }}</span>
                                </div>
                            </div>
                            <div v-else class="font-9 tax-label">
                                Keine Steuerdaten
                            </div>
                        </td>
                        <td class="text-center">
                            <v-btn icon="mdi-pencil" size="small" color="info" @click="emits('editEntry', entry)"
                                class="mr-2" density="compact" variant="tonal" title="Bearbeiten" />
                            <v-btn icon="mdi-delete" size="small" color="error" @click="emits('deleteEntry', entry)"
                                class="mr-2" density="compact" variant="tonal" title="Löschen" />
                        </td>
                    </tr>
                </tbody>
                <tbody v-else>
                    <tr>
                        <td colspan="7" class="text-center py-6">
                            Keine Einträge gefunden
                        </td>
                    </tr>
                </tbody>
            </v-table>

            <div class="d-flex justify-center mt-4">
                <v-pagination v-model="currentPage" :length="totalPages" :total-visible="5"
                    @update:model-value="selectedTags.splice(0)" />
            </div>
        </v-card-text>
    </v-card>
</template>
<script
    setup
    lang="ts"
>
    import { computed, ref } from 'vue';
    import { Dividend, QSt } from '../models';
    import { formatCurrency, formatDate, getFlag, timePeriods } from '../utils';
    import { calculateCreditableTax, calculateRefundableTax, calculateWithholdingTax } from '../tax';
    import { db, updDiv } from '../db';
    import { add, format, lastDayOfMonth, parse } from 'date-fns';
    import AppCardToggle from './incl/AppCardToggle.vue';

    type Emits = {
        (e: 'editEntry', entry: Dividend): void
        (e: 'deleteEntry', entry: Dividend): void
    }

    type Props = {
        tags: string[]
    }

    const entries = defineModel<Dividend[]>({ default: () => [] });
    const emits = defineEmits<Emits>();
    const props = defineProps<Props>();

    const filter = ref('');
    const sortField = ref<keyof Dividend | 'country'>('date');
    const sortDirection = ref(-1); // -1 for descending, 1 for ascending
    const rows = ref(10);
    const currentPage = ref(1);
    const totalPages = computed(() => Math.ceil(filteredEntries.value.length / rows.value));

    const selectedDivs = ref<number[]>([]);
    const selectedTags = ref<string[]>([]);
    const filterByTag = ref<string>('');

    const tableFromLabel = ref('');
    const tableUntilLabel = ref('');

    const hideContent = ref(false);

    const qsts = await db.getQstsAll();
    const getCountryInfo = (isin: string): QSt | null => {
        return qsts.find((qst: QSt) => qst.iso === isin.slice(0, 2).toUpperCase()) || null;
    };

    const tableLabels = computed<Set<string>>(() => {
        const labels: Set<string> = new Set<string>();
        entries.value.slice().sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())
            .forEach(entry => labels.add(format(new Date(entry.date), timePeriods.month.labelFormat)))
        return labels;
    })

    const paginatedEntries = computed(() => {
        const start = (currentPage.value - 1) * rows.value;
        const end = start + rows.value;
        return filteredEntries.value.slice(start, end);
    });

    const filteredEntries = computed(() => {
        let result = [...entries.value];
        result = filterTable(result);
        result = sortTable(result);
        return result;
    });


    const sort = (field: keyof Dividend | 'country') => {
        if (sortField.value === field) {
            sortDirection.value *= -1;
        } else {
            sortField.value = field;
            sortDirection.value = -1;
        }
    };

    const sortIcon = (field: string) => {
        if (sortField.value !== field) return 'mdi-menu-down';
        return sortDirection.value === 1 ? 'mdi-menu-up' : 'mdi-menu-down';
    };

    function bulkEditTags(tags: string | string[] | null, divIds: number[]): void {
        if (Array.isArray(tags)) {
            entries.value.forEach((entry) => {
                if (divIds.includes(entry.id)) {
                    console.debug('change entry', entry.tags, 'to ', tags);
                    if (tags.sort().join('|').toUpperCase() !== entry.tags.sort().join('|').toUpperCase()) {
                        entry.tags = [...tags];
                        updDiv(entry.id, entry);
                    }
                }
            });
        }
    }

    function updateTable() {
        // This function is called when the chart labels are updated
        // It can be used to trigger any additional updates needed for the table
        console.debug('updateTable called');
    }

    function filterTable(result: Dividend[]): Dividend[] {
        if (filter.value) {
            const searchTerm = filter.value.toLowerCase();
            result = result.filter(entry =>
                entry.isin.toLowerCase().includes(searchTerm) ||
                entry.amount.toString().includes(searchTerm) ||
                formatDate(entry.date).includes(searchTerm) ||
                entry.tags.some(tag => tag.toLowerCase().includes(searchTerm))
            );
        }

        if (filterByTag.value) {
            const searchTerm = filterByTag.value;
            result = result.filter(entry =>
                entry.tags.includes(searchTerm)
            );
        }

        if (tableFromLabel.value || tableUntilLabel.value) {
            const dateTableFrom = !tableFromLabel.value ? new Date(0) : parse(tableFromLabel.value, timePeriods.month.labelFormat, new Date());
            const dateTableUntil = lastDayOfMonth(!tableUntilLabel.value ? add(new Date(), { months: 1 }) : parse(tableUntilLabel.value, timePeriods.month.labelFormat, new Date()));

            // console.debug('filter-by-label', dateTableFrom, '-', dateTableUntil);
            result = result.filter(entry => {
                const entryDate = entry.date.getTime();
                console.debug('entryDate', entryDate, dateTableFrom, '-', dateTableUntil, 'match',
                    entryDate >= dateTableFrom.getTime() && entryDate <= dateTableUntil.getTime());
                return entryDate >= dateTableFrom.getTime() && entryDate <= dateTableUntil.getTime();
            });
        }

        return result;
    }

    function sortTable(result: Dividend[]): Dividend[] {
        return result.sort((a: Dividend, b: Dividend) => {
            let valueA: string | string[] | Date | number | null = null;
            let valueB: string | string[] | Date | number | null = null;

            if (sortField.value === 'date') {
                valueA = a[sortField.value];
                valueB = b[sortField.value];
                valueA = new Date(`${valueA}`);
                valueB = new Date(`${valueB}`);
            } else if (sortField.value === 'country') {
                const countryA = getCountryInfo(a.isin)?.name || '';
                const countryB = getCountryInfo(b.isin)?.name || '';
                if (countryA < countryB) return -1 * sortDirection.value;
                if (countryA > countryB) return 1 * sortDirection.value;
                return 0;
            } else {
                valueA = a[sortField.value];
                valueB = b[sortField.value];
            }

            if (valueA < valueB) return -1 * sortDirection.value;
            if (valueA > valueB) return 1 * sortDirection.value;
            return 0;
        });
    }
</script>

<style
    lang="css"
    scoped
>
    .tax-label {
        opacity: 0.6;
    }
</style>