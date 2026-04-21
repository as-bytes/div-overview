<template>
    <v-dialog v-if="isin" :model-value="true" persistent max-width="800px" class="glass-backdrop">
        <v-card :loading="isLoading">

            <v-card-title>ISIN-Daten abfragen für {{ isin }}</v-card-title>
            <v-card-title class="font-12 font-weight-light">
                <!-- v-btn @click="queryIsinByFinn()" color="primary" text="finnHub.com" density="comfortable"
                    :loading="isLoading" />
                <v-btn @click="queryIsinByFigi()" color="primary" text="openFigi.com" density="comfortable"
                    :loading="isLoading" /--->
                Wertpapier-Name über
                <v-btn @click="updateIsinInfoViaGoogle()" color="secondary" text="Web-Suche" :loading="isLoading"
                    variant="tonal" class="font-10" density="comfortable" /> ermitteln
            </v-card-title>
            <v-card-text>
                <div style="width: 270px" class="ml-5">
                    <v-text-field v-model="apiTokenFinnHub" label="Finnhub API-Token" variant="outlined"
                        density="compact" hide-details />
                </div>
                <div style="width: 270px" class="ml-5">
                    <v-text-field v-model="apiTokenOpenFigi" label="OpenFigi API-Token" variant="outlined"
                        density="compact" hide-details />
                </div>

                <v-row>
                    <v-col cols="6">
                        <v-label>Gefundene Namen:</v-label>
                        <v-list height="160" fixed>
                            <div v-if="names.length === 0">Keine Namen gefunden</div>
                            <v-radio-group v-model="selectedName">
                                <v-radio :value="name" :label="name" hide-details v-for="(name, index) in names"
                                    :key="index" density="compact" />
                            </v-radio-group>
                        </v-list>

                    </v-col>
                    <v-col cols="6">
                        <v-label>Name des Wertpapiers:</v-label>
                        <v-text-field v-model="selectedName" variant="filled" />
                        <v-label>Typ des Wertpapiers:</v-label>
                        <v-radio-group inline v-model="selectedType">
                            <v-radio variant="outlined" label="Aktie" density="compact" hide-details
                                value="Common Stock" />
                            <v-radio label="ETC, ETF, ETP, Fond..." hide-details density="compact" class="pl-5"
                                value="ETF" />
                        </v-radio-group>
                    </v-col>
                </v-row>

            </v-card-text>
            <v-card-actions class="mb-1">
                <v-btn @click="resetForm()" color="error" text="Abbrechen" />
                <v-btn :disabled="!canSave" @click="updateIsin()" color="success" text="Speichern" />
            </v-card-actions>
        </v-card>
    </v-dialog>
</template>
<script
    setup
    lang="ts"
>
    import { db } from '@/db';
    import { IsinType } from '@/models';
    import { useMessagesStore } from '@/stores';
    import { computed, ref } from 'vue';

    const isin = defineModel<string>({ default: () => '' });
    const names = ref<string[]>([]);
    const selectedName = ref<string | null>(null);
    const selectedType = ref<IsinType | null>(null);
    const emits = defineEmits(['isin-updated']);
    const msgStore = useMessagesStore();
    const isLoading = ref(false);

    // todo move to config store
    const apiTokenFinnHub = ref('');
    const apiTokenOpenFigi = ref('');

    // const queryIsinByFinn = async (): Promise<void> => {
    //     // Implement the logic to query ISIN data from finnHub
    //     fetch('/api/')
    //     msgStore.showInfo('Abfrage von finnHub.com ist noch nicht implementiert.');
    // }

    // const queryIsinByFigi = async (): Promise<void> => {
    //     // Implement the logic to query ISIN data from openFigi
    //     msgStore.showInfo('Abfrage von openFigi.com ist noch nicht implementiert.');
    // }

    const canSave = computed(() => selectedName.value !== null && selectedType.value !== null && selectedName.value.length > 0 && selectedType.value.length > 0);

    function updateIsinInfoViaGoogle() {
        isLoading.value = true;
        const url = `/api/isin-by-google/?isin=${isin.value}`
        fetch(url)
            .then(response => response.json())
            .then(respJson => {
                console.log('Google-API:', respJson);
                if (respJson.error) {
                    throw new Error(respJson.error);
                } else if (respJson && respJson.data) {
                    console.log('Google-API:', respJson.data);
                    names.value = respJson.data.map(_ => _.replaceAll(' Aktie', ''));
                    if (names.value.length > 0) {
                        selectedName.value = names.value[0].replaceAll(' Aktie', '');
                        selectedType.value = (respJson.data.some(_ => _.includes('Aktie')) ? IsinType.STOCK : IsinType.ETF);
                    }
                    msgStore.showSuccess('ISIN-Daten abgerufen', 1000);
                } else {
                    msgStore.showWarn('keine ISIN-Daten gefunden (z.B. durch Kapitalmaßnahme...)', 1000);
                }
            })
            .catch(error => {
                msgStore.showError('Web-API:', error);
            }).finally(() => {
                isLoading.value = false;
            });
    }


    // function updateIsinInfoViaOpenFigi(isin: string) {
    //     const url = `/api/isin-by-openfigi/?isin=${isin}&token=${apiTokenOpenFigi.value}`
    //     fetch(url)
    //         .then(response => response.json())
    //         .then(respJson => {
    //             console.log('OpenFigi-API:', respJson);
    //             if (respJson.error) {
    //                 throw new Error(respJson.error);
    //             } else if (respJson && respJson.length > 0 && respJson[0].data && respJson[0].data.length > 0 && respJson[0].data[0].name) {
    //                 const entry = {
    //                     isin,
    //                     name: respJson[0].data[0].name,
    //                     type: respJson[0].data[0].securityType
    //                 };
    //                 console.debug('adding ISIN data:', entry);
    //                 db.updIsin(isin, entry);
    //                 msgStore.showSuccess('ISIN-Daten aktualisiert', 1000);
    //             } else {
    //                 msgStore.showWarn('keine ISIN-Daten gefunden (z.B. durch Kapitalmaßnahme...)', 1000);
    //             }
    //         })
    //         .catch(error => {
    //             msgStore.showError('OpenFigi-API:', error);
    //         });
    // }

    // function updateIsinInfoViaFinnHub(isin: string) {
    //     const url = `https://finnhub.io/api/v1/search?q=${isin}&token=${apiTokenFinnHub.value}`
    //     fetch(url)
    //         .then(response => response.json())
    //         .then(data => {
    //             console.log('Finnhub API response:', data);
    //             if (data.error) {
    //                 throw new Error(data.error);
    //             } else if (data.result && data.result.length > 0 && data.result[0]?.description) {
    //                 const entry = {
    //                     isin,
    //                     name: data.result[0]!.description,
    //                     type: data.result[0]!.type
    //                 };
    //                 console.debug('adding ISIN data:', entry);
    //                 db.updIsin(isin, entry);
    //                 msgStore.showSuccess('ISIN-Daten aktualisiert', 1000);
    //             } else {
    //                 msgStore.showWarn('keine ISIN-Daten gefunden (z.B. durch Kapitalmaßnahme...)', 1000);
    //             }
    //         })
    //         .catch(error => {
    //             msgStore.showError('Finnhub-API:', error);
    //         });
    // }

    const resetForm = (): void => {
        isin.value = '';
        names.value = [];
        selectedName.value = null;
        selectedType.value = null;
    };

    const updateIsin = async (): Promise<void> => {
        if (isin.value && selectedName.value && selectedType.value) {
            try {
                await db.updIsin(isin.value, { name: selectedName.value, type: selectedType.value, isin: isin.value });
                emits('isin-updated');
                msgStore.showSuccess('ISIN-Daten aktualisiert');
                resetForm()
            } catch (error) {
                msgStore.showError('Fehler beim Aktualisieren der ISIN:', error);
            }
        }
    }
</script>