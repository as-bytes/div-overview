<template>
    <span @click="toggleCollapse()" class="float-right cursor-pointer">
        <v-icon v-if="getCollapse()" icon="mdi-chevron-up" />
        <v-icon v-else icon="mdi-chevron-down" />
    </span>
</template>

<script
    setup
    lang="ts"
>
    import { useConfigStore } from "../../stores";

    type Props = {
        id: string
    }

    const model = defineModel<boolean>({ default: false });
    const props = defineProps<Props>();

    const configStore = await useConfigStore();

    const toggleCollapse = async (): Promise<void> => {
        const state = await configStore.toggleCollapseState(props.id);
        model.value = state ?? false;
    }

    const getCollapse = (): boolean => {
        model.value = configStore.collapseStates[props.id] ?? false
        return model.value;
    }
</script>