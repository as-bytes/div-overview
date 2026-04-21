<template>
    <v-dialog v-if="show" v-model="show" location="center center" location-strategy="static" width="400">
        <v-card class="mx-auto">
            <VueDatePicker v-model="date" auto-apply class="mx-auto w-100" placeholder="TT.MM.JJJJ" :text-input="false"
                inline required :hide-navigation="['time']" week-numbers="local" week-start="1"
                :enable-time-picker="false" :dark="configStore.getDarkMode()" format="dd.MM.yyyy" locale="de-DE"
                week-num-name="KW" @update:model-value="show = false" />
        </v-card>
    </v-dialog>
</template>

<script
    setup
    lang="ts"
>
    import { useConfigStore } from "../../stores";
    const configStore = await useConfigStore();
    const show = defineModel<boolean>('show', { default: () => false });
    const date = defineModel<Date | null>('date', { default: () => new Date() });
</script>