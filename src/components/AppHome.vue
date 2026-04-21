<template>
  <Suspense>
    <AppHeader @importDataJson="importJson" v-model="entries" />
  </Suspense>
  <Suspense>
    <AppChartsByTime v-model="entries"></AppChartsByTime>
  </Suspense>
  <Suspense>
    <AppChartsByIsin v-model="entries"></AppChartsByIsin>
  </Suspense>
  <Suspense>
    <AppDataInput v-model="entries" :entry-to-edit="entryToEdit" :tags="tags" />
  </Suspense>
  <Suspense>
    <AppTableEntries v-model="entries" :tags="tags" @editEntry="entryToEdit = $event" @deleteEntry="deleteEntry" />
  </Suspense>
  <Suspense>
    <AppTableIsins v-model="entries" />
  </Suspense>
  <Suspense>
    <AppTableCountries v-model="entries" />
  </Suspense>

  <div class="footer text-center mt-6">
    <v-divider class="pb-3" />
    Quellensteuer-Erstattung App &copy; {{ new Date().getFullYear() }} | Alle Daten werden lokal in Ihrem Browser
    gespeichert
  </div>

  <input type="file" ref="fileInput" style=" display: none" @change="handleUpload" accept=".json">
  Debugging: {{ $vuetify.display.width }}
</template>

<script
  setup
  lang="ts"
>
  import AppTableEntries from "./AppTableEntries.vue";
  import AppTableIsins from "./AppTableIsins.vue";
  import AppTableCountries from "./AppTableCountries.vue";
  import AppHeader from "./AppHeader.vue";
  import AppDataInput from "./AppDataInput.vue";
  import AppChartsByIsin from "./AppChartsByIsin.vue";
  import AppChartsByTime from "./AppChartsByTime.vue";
  import { ref, computed, onMounted, useTemplateRef } from "vue";
  import { Dividend, EntryNew } from "../models";
  import { delDiv, getDivs } from "../db";
  import { importDataJson } from "../export-import";
  import { useMessagesStore } from "../stores";

  const entries = ref<Dividend[]>([]);

  const fileInput = useTemplateRef('fileInput');

  const tags = computed(() => [...new Set(entries.value.flatMap((_) => _.tags))].sort());

  const entryToEdit = ref<EntryNew | null>(null);

  const handleUpload = async (event: Event): Promise<void> => {
    const data = await importDataJson(event);
    if (data !== null) {
      entries.value = data.divs;
    }
  }

  const deleteEntry = async (entry: Dividend) => {
    if (confirm('Möchten Sie diesen Eintrag wirklich löschen?')) {
      try {
        entries.value = await delDiv(entry.id);
      } catch (error) {
        console.error('Fehler beim Löschen des Eintrags:', error);
      }
    }
  };

  const importJson = () => {
    if (fileInput.value) fileInput.value!.click();
  };

  // function api() {
  //   fetch("/api/")
  //     .then((res) => res.json() as Promise<{ name: string }>)
  //     .then((data) => console.log(data.name));
  // }

  onMounted(async () => {
    try {
      entries.value = await getDivs();
    } catch (error) {
      useMessagesStore().showError('Fehler beim Laden der Daten:', error);
    }
  });
</script>

<style scoped>
  * {
    font-size: 10pt;
  }
</style>
