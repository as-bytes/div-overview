<template>
    <v-dialog :model-value="true" location="center center" location-strategy="static" width="1000" min-height="70%"
        persistent>
        <v-card class="mx-auto w-100">
            <v-card-title>
                Quellensteuer-Tabelle bearbeiten
                <v-btn variant="text" icon="mdi-close" density="compact" @click="emits('close')" class="float-right" />
            </v-card-title>
            <v-card-text>
                <v-table striped="even" height="100%" fixed-header class="w-100">
                    <thead>
                        <tr>
                            <th class="text-left" style="width: 110px">ISO</th>
                            <th class="text-left">Land</th>
                            <th class="text-left" style="width: 140px">Steuerrate</th>
                            <th class="text-left" style="width: 140px">Anrechenbar</th>
                            <th class="text-left" style="width: 140px">Erstattungsfähig</th>
                            <th style="width: 120px">
                                Aktionen
                                <v-btn variant="text" icon color="success" size="small" density="compact"
                                    @click="showNewEntry = !showNewEntry">
                                    <v-icon icon="mdi-plus" />
                                    <v-tooltip activator="parent" location="top" open-delay="300">Neuer
                                        Eintrag</v-tooltip>
                                </v-btn>
                            </th>
                        </tr>
                    </thead>
                    <tbody style="max-height: 200px; overflow-y: auto;">
                        <tr v-if="showNewEntry">
                            <td>
                                <AppIsinTextFeld v-model="newEntry.iso!" :show-label="false" placeholder="e.g. DE"
                                    density="compact" />
                            </td>
                            <td><v-text-field v-model="newEntry.name" width="200" density="compact" hide-details
                                    variant="underlined" />
                            </td>
                            <td><v-text-field type="number" v-model.number="newEntry.taxRate" class="input-right"
                                    density="compact" hide-details variant="underlined">
                                    <template #append-inner>%</template></v-text-field>
                            </td>
                            <td><v-text-field type="number" v-model.number="newEntry.creditable" class="input-right"
                                    density="compact" variant="underlined" hide-details><template
                                        #append-inner>%</template></v-text-field></td>
                            <td><v-text-field type="number" v-model.number="newEntry.refundable" class="input-right"
                                    density="compact" variant="underlined" hide-details><template
                                        #append-inner>%</template></v-text-field></td>
                            <td>
                                <v-btn variant="text" icon="mdi-floppy" density="comfortable" color="success"
                                    @click="addNewEntry()"></v-btn>
                            </td>
                        </tr>
                        <tr v-for="(qst, index) in qsts" :key="qst.iso">
                            <td>
                                <AppIsinTextFeld v-model="qst.iso" :show-label="false" placeholder="DE"
                                    density="compact" />
                            </td>
                            <td><v-text-field v-model="qst.name" width="200" density="compact" hide-details
                                    variant="underlined" />
                            </td>
                            <td><v-text-field type="number" v-model.number="qst.taxRate" class="input-right"
                                    density="compact" hide-details variant="underlined">
                                    <template #append-inner>%</template></v-text-field>
                            </td>
                            <td><v-text-field type="number" v-model.number="qst.creditable" class="input-right"
                                    density="compact" variant="underlined" hide-details><template
                                        #append-inner>%</template></v-text-field></td>
                            <td><v-text-field type="number" v-model.number="qst.refundable" class="input-right"
                                    density="compact" variant="underlined" hide-details><template
                                        #append-inner>%</template></v-text-field></td>
                            <td>
                                <span v-if="confirmDelete !== index">
                                    <v-btn variant="text" icon="mdi-floppy" color="success"
                                        density="comfortable"></v-btn>
                                    <v-btn variant="text" icon="mdi-trash-can" color="error" density="comfortable"
                                        @click="confirmDelete = index"></v-btn>
                                </span>
                                <span v-else>
                                    <v-btn variant="text" icon="mdi-check" color="success" @click="del(qst)"
                                        density="comfortable"></v-btn>
                                    <v-btn variant="text" icon="mdi-close" color="error" density="comfortable"
                                        @click="resetConfirmDelete()"></v-btn>
                                </span>
                            </td>
                        </tr>
                    </tbody>
                </v-table>
            </v-card-text>
        </v-card>
    </v-dialog>
</template>

<script
    setup
    lang="ts"
>
    import { ref } from 'vue';
    import { db } from '../db';
    import { type QSt } from '../models';
    import AppIsinTextFeld from './incl/AppIsinTextFeld.vue';

    const qsts = ref<QSt[]>([]);
    const confirmDelete = ref(-1);
    const showNewEntry = ref(false);

    const emits = defineEmits(['close']);

    const newEntry = ref<Partial<QSt>>(getNewEntry());

    const resetConfirmDelete = (): number => confirmDelete.value = -1;

    updateQsts();

    function updateQsts() {
        db.getQstAll().then(entries => {
            qsts.value = entries;
        });
    }

    function getNewEntry(): Partial<QSt> {
        return {
            iso: '',
            name: '',
            taxRate: 0,
            creditable: 0,
            refundable: 0
        };
    }

    async function addNewEntry() {
        resetConfirmDelete();
        await db.addQst(newEntry.value as QSt);
        newEntry.value = {};
        showNewEntry.value = false;
        updateQsts();
    }

    async function del(entry: QSt): Promise<void> {
        resetConfirmDelete();
        await db.delQst(entry.iso);
    }

    // async function _upd(entry: QSt): Promise<void> {
    //     resetConfirmDelete();
    //     await db.updQst(entry.iso, entry);
    // }
</script>

<style scoped>
    .input-right :deep(input) {
        text-align: right;
    }
</style>