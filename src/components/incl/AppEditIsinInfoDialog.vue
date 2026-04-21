<template>
    <v-dialog v-if="entry !== null" :model-value="true" persistent max-width="500px">
        <v-card>
            <v-card-title>ISIN-Daten bearbeiten</v-card-title>
            <v-card-text>
                <v-text-field v-model="entry.isin" label="ISIN (nicht editierbar)" variant="outlined" disabled />
                <v-text-field v-model="entry.name" label="Bezeichnung" variant="outlined" />
                <v-label>Art des Wertpapiers</v-label>
                <v-checkbox v-model="entry.type" variant="outlined" label="Aktie" density="compact" hide-details
                    value="Common Stock" />
                <v-checkbox v-model="entry.type" label="ETC, ETF, ETP, Fond..." hide-details density="compact"
                    value="ETF" />
            </v-card-text>
            <v-card-actions class="mb-1">
                <v-btn @click="entry = null" color="error" text="Abbrechen" />
                <v-btn :disabled="entry.isin.length === 0 || entry.name.length === 0 || !entry.type"
                    @click="updateIsin()" color="success" text="Speichern" />
            </v-card-actions>
        </v-card>
    </v-dialog>
</template>
<script
    setup
    lang="ts"
>
    import { db } from '@/db';
    import { Isin } from '@/models';
    import { useMessagesStore } from '@/stores';

    const entry = defineModel<Isin | null>({ default: () => null });
    const emits = defineEmits(['isin-updated']);
    const msg = useMessagesStore();

    const updateIsin = async (): Promise<void> => {
        if (entry.value) {
            try {
                await db.updIsin(entry.value.isin, entry.value);
                emits('isin-updated');
                msg.showSuccess('ISIN-Daten aktualisiert');
                entry.value = null;
            } catch (error) {
                msg.showError('Fehler beim Aktualisieren der ISIN:', error);
            }
        }
    }
</script>