<template>
    <v-card class="mb-6" :elevation="10">
        <v-card-title>
            Neuen Eintrag hinzufügen
            <Suspense>
                <AppCardToggle v-model="hideContent" id="Neuen-Eintrag-hinzufügen" />
            </Suspense>
        </v-card-title>
        <v-card-text class="mt-3 pb-3" v-show="!hideContent">
            <v-form @submit.prevent="currentlyEditing ? updateEntry() : addEntry()">
                <v-virtual-scroll min-height="50" max-height="500" :items="newEntries"
                    class="pt-1 responsive-min-height">
                    <template v-slot:default="{ item: newEntry, index }">
                        <v-row dense class="input-row">
                            <v-col cols="12" md="3">
                                <AppIsinTextFeld v-model="newEntries[index].isin" :show-label="index === 0"
                                    @update:modelValue="newEntries[index].isin = $event?.slice(0, 12)" label-text="ISIN"
                                    placeholder="DE0007164600" />
                            </v-col>
                            <v-col cols="12" md="3">
                                <v-text-field v-model.number="newEntry.amount" placeholder="100.00" :rules="[required]"
                                    persistent-placeholder type="number" step="0.01" required density="compact"
                                    variant="underlined" hide-details class="amount-text">
                                    <template #label v-if="index === 0">
                                        Betrag
                                    </template>
                                    <template #append-inner>
                                        €
                                    </template>
                                </v-text-field>
                            </v-col>
                            <v-col cols="12" md="3">
                                <Suspense>
                                    <AppDatePicker v-model:show="pickDate" v-model:date="newEntry.date" />
                                </Suspense>
                                <Suspense>
                                    <AppDateInput v-model:date="newEntry.date" @pick-date="pickDate = true"
                                        variant="underlined" :show-label='index === 0' />
                                </Suspense>
                            </v-col>
                            <v-col cols="12" md="3">
                                <v-combobox v-model="newEntry.tags" :items="props.tags" multiple density="compact"
                                    @click:append-inner.prevent.stop="" variant="underlined" placeholder="z.B. Depot1"
                                    hide-details chips closable-chips>
                                    <template #label v-if="index === 0">
                                        Tags
                                    </template>
                                    <template #append-inner>
                                        <span v-if="index === 0 && newEntries.length > 1">
                                            <v-icon icon="mdi-content-copy" size="small"
                                                class="cursor-pointer mr-1 mb-1"
                                                @click.stop.prevent="cloneTags(newEntry, newEntries)" />
                                            <v-tooltip activator="parent" location="top" open-delay="300">Aktuelle Tags
                                                auf
                                                alle anderen Einträge kopieren</v-tooltip>
                                        </span>
                                        <v-icon icon="mdi-trash-can" size="small" class="cursor-pointer"
                                            @click.stop.prevent="newEntries.splice(index, 1)" />
                                        <v-tooltip activator="parent" location="top" open-delay="300">Eintrag
                                            entfernen</v-tooltip>
                                    </template>
                                </v-combobox>
                            </v-col>
                        </v-row>
                    </template>
                </v-virtual-scroll>
                <div class="float-left mb-2">
                    <v-btn v-if="!currentlyEditing" size="small" variant="text" class="clipboard-hint mt-2"
                        color="primary" density="comfortable" @click="addFromClipboard()"
                        prepend-icon="mdi-content-paste">
                        Freitext aus Zwischenablage einfügen
                    </v-btn>
                    <br />
                    <div v-if="!currentlyEditing">
                        <v-btn size="small" variant="text" class="clipboard-hint" color="primary" density="comfortable"
                            @click="batchAddFromCsv()" prepend-icon="mdi-content-paste">
                            <template #default>
                                CSV aus Zwischenablage einfügen (Max. {{ maxEntriesToAdd }} Einträge)
                            </template>
                            <template #append>
                                <v-checkbox @click.prevent.stop="skipDupes = !skipDupes" density="compact" hide-details
                                    title="Duplikate ignorieren" v-model="skipDupes" class="pl-2" style="zoom: 0.8">
                                    <template #label>
                                        <span v-show="$vuetify.display.width >= 750">
                                            Duplikate ignorieren
                                        </span>
                                    </template>
                                </v-checkbox>
                            </template>
                        </v-btn>
                    </div>
                </div>
                <div class="float-right my-2">
                    <v-btn :color="currentlyEditing ? 'warning' : 'success'" type="submit" size="small"
                        :prepend-icon="currentlyEditing ? 'mdi-content-save' : 'mdi-plus'">
                        {{ newEntries.length }} {{ currentlyEditing ? 'Aktualisieren' : 'Hinzufügen' }}
                    </v-btn>
                    <v-btn v-if="currentlyEditing" class="ml-2" size="small" color="secondary" @click="resetForm"
                        prepend-icon="mdi-close">
                        Abbrechen
                    </v-btn>
                </div>
            </v-form>
        </v-card-text>
    </v-card>
</template>

<script
    setup
    lang="ts"
>
    import { ref, watch } from 'vue';
    import { pasteCsvFromClipboard, pasteAnyFromClipboard } from '../clipboard';
    import { addDivMany, getDivs, updDiv } from '../db';
    import { required } from '../rules';
    import { Dividend, EntryNew } from '../models';
    import { useMessagesStore } from '../stores';
    import { formatDate, getEmptyEntry } from '../utils';
    import AppDateInput from './incl/AppDateInput.vue';
    import AppDatePicker from './incl/AppDatePicker.vue';
    import AppIsinTextFeld from './incl/AppIsinTextFeld.vue';
    import AppCardToggle from './incl/AppCardToggle.vue';

    type Props = {
        'entryToEdit': EntryNew | null
        tags: string[]
    }

    const maxEntriesToAdd = 1500;

    const entries = defineModel<Dividend[]>({ default: () => [] });
    const newEntries = ref<EntryNew[]>([getEmptyEntry()]);

    const props = defineProps<Props>();

    // const configStore = await useConfigStore();
    const msgStore = await useMessagesStore();

    const currentlyEditing = ref(false);
    const skipDupes = ref(true);
    const pickDate = ref(false);

    const hideContent = ref(false);

    watch(() => props.entryToEdit, () => {
        if (props.entryToEdit) {
            editEntry(props.entryToEdit);
        }
    })

    async function batchAddFromCsv(): Promise<void> {
        const parsedDivs = await pasteCsvFromClipboard(maxEntriesToAdd, skipDupes.value);
        if (parsedDivs.length > 0) {
            newEntries.value = parsedDivs;
        }
    }

    async function addFromClipboard(): Promise<void> {
        const newEntry = await pasteAnyFromClipboard();
        if (newEntry) {
            if (newEntries.value.length === 1 && !newEntries.value[0].isin && !newEntries.value[0].amount) {
                newEntries.value[0] = { ...newEntry };
            } else {
                newEntries.value.push(newEntry);
            }
        }
    }

    const divExists = (entry: EntryNew): boolean => {
        return entries.value.some(_ => {
            const sameIsin = _.isin === entry.isin;
            const sameAmount = _.amount === entry.amount;
            const sameDate = formatDate(_.date) === formatDate(entry.date);
            return sameIsin && sameAmount && sameDate;
        });
    }


    const addEntry = async (ignoreDupes = true) => {
        if (newEntries.value.length < 1 || (newEntries.value.some((newEntry) => !newEntry.isin || !newEntry.amount || !newEntry.date))) {
            msgStore.showInfo('Bitte füllen Sie alle Pflichtfelder aus (ISIN, Betrag, Datum)')
            return;
        }

        try {
            const unique: Dividend[] = [];
            const dupes: Dividend[] = []

            newEntries.value.forEach(entry => {
                if (divExists(entry)) {
                    dupes.push({ ...entry } as Dividend);
                } else {
                    unique.push({ ...entry } as Dividend);
                }
            })

            if (dupes.length > 0 && !ignoreDupes && confirm(`${unique.length} Einträge mit denselben Werten existieren bereits, trotzdem hinzufügen?`)) {
                await addDivMany([...unique, ...dupes]);
            } else {
                await addDivMany([...unique]);
            }

            if (dupes.length > 0) {
                msgStore.showInfo(`${unique.length} neue Einträge hinzugefügt und ${dupes.length} Einträge aktualisiert`)
            } else {
                msgStore.showInfo(`${unique.length} neue Einträge hinzugefügt`)
            }
        } catch (error) {
            console.error('Fehler beim Hinzufügen des Eintrags:', error);
        } finally {
            resetForm();
            entries.value = await getDivs();
        }
    };


    const updateEntry = async () => {
        if (newEntries.value.length < 1) {
            return msgStore.showWarn('Bitte füllen Sie alle Pflichtfelder aus (ISIN, Betrag, Datum)');
        }

        const newEntry = newEntries.value[0]!;
        if (!newEntry.isin || !newEntry.amount || !newEntry.date) {
            return msgStore.showWarn('Bitte füllen Sie alle Pflichtfelder aus (ISIN, Betrag, Datum)');
        }

        const entryToUpdate = { ...newEntry } as Dividend;

        try {
            // todo enforcing id! is meh
            await updDiv(newEntry.id!, entryToUpdate);
            const index = entries.value.findIndex(e => e.id === newEntry.id);
            if (index !== -1) {
                entries.value[index] = { ...entries.value[index], ...entryToUpdate };
            }

            resetForm();
        } catch (error) {
            msgStore.showError('Fehler beim Aktualisieren des Eintrags:', error)
        }
    };


    const resetForm = (): void => {
        newEntries.value = [getEmptyEntry()];
        currentlyEditing.value = false;
    };

    const editEntry = (entry: EntryNew) => {
        currentlyEditing.value = true;
        newEntries.value = [{ ...entry }]
    };

    function cloneTags(entry: EntryNew, entries: EntryNew[]) {
        entries.forEach(_ => _.tags = [...entry.tags]);
        msgStore.showSuccess('Alle Tags aktualisiert', 1000);
    }
</script>


<style scoped>
    .isin-text :deep(input) {
        text-align: left
    }

    .amount-text :deep(input) {
        text-align: right
    }

    .amount-text :deep(.v-label) {
        width: 100% !important;
        text-align: right;
        padding-right: 25px;

    }

    .date-text :deep(input) {
        text-align: center;
    }

    .date-text :deep(.v-label) {
        width: 100% !important;
        text-align: center;
        padding-right: 25px;

    }

    .input-row:hover :deep(.v-input) {
        border-bottom: 1px solid yellow;
    }

    .input-row :deep(.v-input) {
        border-bottom: 1px solid transparent;
    }

    .v-virtual-scroll__item:nth-child(even) .input-row {
        background-color: rgba(0, 0, 0, 0.15);
    }

    @media (max-width: 750px) {
        .input-row {
            height: 50px;
            max-height: 50px;
            width: calc(100%);
        }
    }


    @media (max-width: 750px) {
        .responsive-min-height {
            min-height: 180px !important;
        }
    }

</style>