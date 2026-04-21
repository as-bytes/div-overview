<template>
    <v-text-field :value="displayValue" :display-format="formatDate" hide-details placeholder="TT.MM.JJJJ"
        persistent-placeholder @update:model-value="onUpdate" @blur="onBlurClearInvalidText" autocomplete="off"
        :rules="[required]" class="date-text" required max="10" density="compact" append-inner-icon="mdi-calendar"
        variant="solo-filled" @click:append-inner.prevent="emits('pick-date')">
        <template v-if="props.showLabel" #label>
            Zahlungsdatum
        </template>
    </v-text-field>
</template>

<script
    setup
    lang="ts"
>
    import { ref, watch } from 'vue';
    import { format } from 'date-fns';
    import { concatDateWithDots, dateIsValid, formatDate } from '../../utils'

    type Props = {
        showLabel: boolean
    }

    const modelValue = defineModel<Date | null | undefined>('date', { default: () => new Date() });
    const emits = defineEmits(['pick-date']);
    const props = defineProps<Props>();

    const displayValue = ref('')
    const localeFormat = 'dd.MM.yyyy';

    const onUpdate = (value: string) => {
        displayValue.value = concatDateWithDots(value).slice(0, 10);
        const date = dateIsValid(displayValue.value, localeFormat);
        if (date) {
            modelValue.value = date;
        }

        if (displayValue.value.length === 0) {
            modelValue.value = null;
        }
    };

    watch(() => modelValue.value, () => {
        if (modelValue.value && dateIsValid(modelValue.value, localeFormat)) {
            displayValue.value = format(modelValue.value, localeFormat)
        }
    }, { immediate: true })

    const onBlurClearInvalidText = () => displayValue.value = !dateIsValid(displayValue.value, localeFormat) ? '' : displayValue.value;

    const required = (value: unknown): boolean => !!value;

</script>

<style scoped></style>