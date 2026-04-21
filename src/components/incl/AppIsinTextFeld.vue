<template>
    <v-text-field v-model="isin" :placeholder="props.placeholder" persistent-placeholder required hide-details
        autocomplete="off" density="compact" :rules="[required]" class="isin-text" variant="underlined">
        <template #prepend-inner>
            <img class="mr-2" v-if="countryInfo?.iso" :src="getFlag(countryInfo)" style="width: 40px; height: 27px;" />
        </template>
        <template #label v-if="props.showLabel">
            <span v-html="countryInfo ? `${props.labelText} (${countryInfo.name})` : props.labelText">
            </span>
        </template>
    </v-text-field>
</template>

<script
    setup
    lang="ts"
>
    import { required } from '@/rules';
    import { getFlag } from '@/utils';
    import { db } from '@/db';
    import { ref, watch } from 'vue';
    import { QSt } from '@/models';

    type Props = {
        showLabel: boolean
        labelText?: string
        placeholder: string
    }

    const props = defineProps<Props>();

    const isin = defineModel<string>({ default: () => '', required: true });

    const countryInfo = ref<QSt | null | undefined>(null);

    watch(isin, async (newIsin) => {
        countryInfo.value = (!newIsin) ? null : await db.getQst(newIsin);
    }, { immediate: true });

</script>