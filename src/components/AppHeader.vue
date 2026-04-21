<template>
    <v-card density="compact" class="mb-6">
        <v-card-text class="d-flex justify-space-between align-center card-title-responsive px-2">
            <h1 class="text-h5">
                <v-menu>
                    <template v-slot:activator="{ props }">
                        <v-btn icon v-bind="props" variant="text" density="compact">
                            <v-icon>mdi-dots-vertical</v-icon>
                        </v-btn>
                    </template>
                    <v-list>
                        <v-list-item @click="toggleDarkMode">
                            <v-list-item-title>
                                <span
                                    v-if="configStore.getDarkMode()"><v-icon>mdi-weather-sunny</v-icon>&nbsp;Hell</span>
                                <span v-else><v-icon>mdi-weather-night</v-icon>&nbsp;Dunkel</span>
                            </v-list-item-title>
                        </v-list-item>
                        <v-divider></v-divider>
                        <v-list-item @click="exportDataJson()">
                            <v-list-item-title>
                                <v-icon>mdi-download</v-icon> Exportieren (JSON)
                            </v-list-item-title>
                        </v-list-item>
                        <v-list-item @click="emits('importDataJson')">
                            <v-list-item-title>
                                <v-icon>mdi-upload</v-icon> Importieren (JSON)
                            </v-list-item-title>
                        </v-list-item>
                        <v-divider></v-divider>
                        <v-list-item @click="editQst = true">
                            <v-list-item-title>
                                <span><v-icon>mdi-table</v-icon>&nbsp;QSt bearbeiten</span>
                            </v-list-item-title>
                        </v-list-item>
                        <v-divider></v-divider>
                        <v-list-item @click="purgeDb">
                            <v-list-item-title>
                                <span class="text-red"><v-icon>mdi-alert</v-icon>&nbsp;DB löschen</span>
                            </v-list-item-title>
                        </v-list-item>
                    </v-list>
                </v-menu>
                Quellensteuer-Erstattung
            </h1>
            <div class="float-right mr-5">
                <v-row class="summary-row" dense>
                    <div class="text-left pa-0">
                        <div class="summary-value">{{ formatCurrency(totalDividends) }}</div>
                        <div class="summary-label">Dividenden</div>
                    </div>
                    <div class="text-center pa-0">
                        <div class="summary-value">{{ entries.length }}</div>
                        <div class="summary-label">Einträge</div>
                    </div>
                    <div class="text-right pa-0">
                        <div class="summary-value">{{ formatCurrency(totalRefundableTax) }}</div>
                        <div class="summary-label">Erstattungsfähig</div>
                    </div>
                </v-row>
            </div>
        </v-card-text>
    </v-card>
    <AppDialogQstEditor v-if="editQst" @close="editQst = false" />
</template>

<script
    setup
    lang="ts"
>
    import { delDb } from '../db';
    import { exportDataJson } from '../export-import';
    import { useConfigStore, useMessagesStore } from '../stores';
    import { computed, ref } from 'vue';
    import { Dividend } from '../models';
    import AppDialogQstEditor from './AppDialogQstEditor.vue';
    import { formatCurrency } from '../utils';

    const entries = defineModel<Dividend[]>({ default: () => [] });

    const totalDividends = computed<number>(() => entries.value.reduce((sum, entry) => sum + entry.amount, 0));
    // const totalWithholdingTax = computed(() => entries\.value.reduce((sum, entry) => sum + calculateWithholdingTax(entry), 0).toFixed(2));
    // const totalRefundableTax = computed<number>(() => entries.value.reduce((sum, entry) => sum + calculateRefundableTax(entry), 0));
    const totalRefundableTax = computed<number>(() => entries.value.reduce((sum, _entry) => sum + 0, 0));

    const editQst = ref(false);

    type Emits = {
        (e: 'importDataJson'): void
    }

    const emits = defineEmits<Emits>();

    const configStore = await useConfigStore();

    const toggleDarkMode = async (): Promise<void> => await configStore.toggleDarkMode();

    const purgeDb = async () => {
        if (confirm('Alle Daten löschen?')) {
            await delDb();
            entries.value.splice(0);
            useMessagesStore().showInfo('Alle Einträge wurden gelöscht.');
        }
    }

</script>

<style scoped>
    /* h3 {
        font-weight: normal;
    } */

    .summary-row>div:nth-child(1) {
        text-align: left;
        width: 160px;
    }

    .summary-row>div:nth-child(2) {
        text-align: center;
        width: 80px;
    }

    .summary-row>div:nth-child(3) {
        text-align: right;
        width: 160px;
    }

    @media screen and (max-width: 750px) {
        h3:before {
            content: "QSt-Erstattung";
        }

        .summary-row>div:nth-child(1) {
            text-align: left !important;
            padding: 1px 1px 10px 14px !important;
        }

        .summary-row>div:nth-child(2) {
            text-align: left !important;
            padding: 11px 1px 10px 14px !important;
        }

        .summary-row>div:nth-child(3) {
            text-align: left !important;
            padding: 11px 10px 20px 14px !important;
        }
    }

    @media screen and (min-width: 750px) {
        h3:before {
            content: "Quellensteuer-Erstattung";
        }
    }

    .card-title-responsive {
        display: flex;
        flex-wrap: wrap;
        align-items: center;
    }
</style>