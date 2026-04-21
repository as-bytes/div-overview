<script
  setup
  lang="ts"
>
  import { onBeforeMount, ref } from 'vue';
  import AppHome from './AppHome.vue';
  import { ConfigStoreActions, ConfigStoreData, useConfigStore, useMessagesStore } from "../stores";

  const snackStore = useMessagesStore();
  const configStore = ref<null | ConfigStoreActions & ConfigStoreData>(null);

  onBeforeMount(() => useConfigStore().then((store) => configStore.value = store));
</script>

<template>
  <v-app :theme="configStore?.initialized && configStore.getDarkMode() ? 'dark' : 'light'" class="bg-black">
    <v-main>
      <div class="app-container">
        <div v-if="configStore != null && configStore.initialized">
          <suspense>
            <AppHome />
          </suspense>
        </div>
        <v-progress-circular v-else indeterminate color="primary" class="mx-auto" />
        <v-snackbar-queue v-model="snackStore.queue" closable="true" timer="true" close-text="X" />
      </div>
    </v-main>
  </v-app>
</template>
