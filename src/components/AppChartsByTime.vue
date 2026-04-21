<template>
    <v-card class="my-6" :elevation="10">
        <v-card-title>
            Diagramme
            <span class="chart-title font-11">
                - Kumulative Dividenden über die Zeit
            </span>
            <div class="float-right">
                <v-progress-circular v-if="isDrawing" indeterminate size="26" color="primary" />
                <Suspense>
                    <AppCardToggle v-model="hideContent" id="Kum-Div-über-Zeit" />
                </Suspense>
            </div>
        </v-card-title>
        <v-card-subtitle class="chart-title" v-show="!hideContent">
            <v-row>
                <v-col>
                    <v-row dense>
                        <v-col cols="6">
                            <v-select v-model="chartLabelLeft" label="Von" density="compact" hide-details
                                @update:model-value="updateCharts()" variant="solo-filled" :items="[...chartLabels]"
                                style="zoom: 0.8" />
                        </v-col>
                        <v-col cols="6">
                            <v-select v-model="chartLabelRight" label="Bis" density="compact" hide-details
                                @update:model-value="updateCharts()" variant="solo-filled" :items="[...chartLabels]"
                                style="zoom: 0.8" />
                        </v-col>
                    </v-row>
                </v-col>
                <v-col>
                    <span class="chart-actions float-right" style="display: flex; gap: 8px;">
                        <v-btn :disabled="isDrawing" @click="setTotalChartResolution(val)"
                            v-for="(key, val) in timePeriods" :key="key.title"
                            :color="totalChartResolution === val ? 'primary' : ''">{{
                                key.title
                            }}</v-btn>
                    </span>
                </v-col>
            </v-row>
        </v-card-subtitle>
        <v-card-text class="chart-container" v-show="!hideContent">
            <canvas ref="totalChartCanvas"></canvas>
        </v-card-text>
    </v-card>
</template>

<script
    setup
    lang="ts"
>
    import { ChartPeriodData, Dividend, PeriodData, TimePeriod } from '../models';
    import { onMounted, ref } from 'vue';
    import { add, format } from 'date-fns';
    import { Chart, ChartConfiguration, ChartDataset, registerables } from 'chart.js';
    import { stackedSumPlugin, timePeriods } from '../utils';
    import AppCardToggle from './incl/AppCardToggle.vue';

    // todo tree shake?
    Chart.register(...registerables);

    const entries = defineModel<Dividend[]>({ default: () => [] });

    const totalChartCanvas = ref<HTMLCanvasElement | null>(null);
    const totalChart = ref<Chart | null>(null);
    const totalChartResolution = ref<TimePeriod>('month');

    const totalColumnCount = ref(0);

    const isDrawing = ref(false);

    const chartLabelRight = ref('');
    const chartLabelLeft = ref('');
    const chartLabels = ref<Set<string>>(new Set<string>());

    const hideContent = ref(false);

    const setTotalChartResolution = (resolution: TimePeriod) => updateTotalChart(totalChartResolution.value, resolution);
    const updateCharts = () => updateTotalChart(totalChartResolution.value);
    const sortDatesDescending = (a: Dividend, b: Dividend) => b.date.getTime() - a.date.getTime();
    const sortDatesAscending = (a: Dividend, b: Dividend) => a.date.getTime() - b.date.getTime();

    const updateSelectLabels = (sortedEntries: Dividend[], timePeriod: TimePeriod) => {
        chartLabels.value.clear();
        chartLabelRight.value = '';
        chartLabelLeft.value = '';

        sortedEntries.forEach(entry => {
            const resolution = timePeriods[timePeriod] ?? timePeriods.month;
            chartLabels.value.add(format(new Date(entry.date), resolution.labelFormat));
        });
    }

    const addEntryToPeriodData = (periodData: Record<string, ChartPeriodData>, resolution: PeriodData, entry: Dividend): void => {
        const date = new Date(entry.date);
        const periodKey = format(date, resolution.keyFormat);
        const label = format(date, resolution.labelFormat);

        if (!periodData[periodKey]) {
            periodData[periodKey] = {
                label: label,
                date: periodKey,
                totals: {},
                tags: [],
            };
        }

        entry.tags.forEach(tag => {
            if (!periodData[periodKey].totals[tag]) {
                periodData[periodKey].totals[tag] = 0;
            }

            periodData[periodKey].totals[tag] += entry.amount;
        });
    }

    const updateTotalChart = (timePeriod: TimePeriod, resolution: TimePeriod = 'month') => {
        if (!totalChartCanvas.value || isDrawing.value) return;
        isDrawing.value = true;

        const periodData: Record<string, ChartPeriodData> = {};
        const period: PeriodData = timePeriods[timePeriod] ?? timePeriods.month;
        entries.value.slice().sort(sortDatesAscending).forEach(entry => {
            addEntryToPeriodData(periodData, period, entry);
        });

        const timeResolutionHasChanged = totalChartResolution.value !== resolution;
        const needsLabelUpdate = timeResolutionHasChanged || chartLabels.value.size === 0;
        if (needsLabelUpdate) {
            updateSelectLabels(entries.value.slice().sort(sortDatesDescending), timePeriod);

            const allLabels = Object.values(periodData).map(period => period.label);
            if (timePeriod === 'week' && !chartLabelLeft.value) {
                chartLabelLeft.value = format(add(new Date(), { weeks: -10 }), timePeriods[timePeriod].labelFormat);
            } else if (timePeriod === 'month' && !chartLabelLeft.value) {
                chartLabelLeft.value = format(add(new Date(), { months: -12 }), timePeriods[timePeriod].labelFormat);
            } else {
                chartLabelLeft.value = allLabels[0] || '';
            }

            chartLabelRight.value = allLabels[allLabels.length - 1] || '';
            totalColumnCount.value = allLabels.length;
        }

        totalChartResolution.value = resolution;


        // second run for actual set labels
        let matchedFromLabel = false;
        let matchedUntilLabel = true;

        const filteredPeriodData = Object.values(periodData).filter(period => {

            const noLabelSelectedOrCurrentSelected = !matchedFromLabel && (chartLabelLeft.value.length === 0 || period.label === chartLabelLeft.value);
            console.debug('chartLabelLeft', chartLabelLeft.value);
            if (noLabelSelectedOrCurrentSelected) {
                matchedFromLabel = true;
            }


            const currentEntryMatchesUntil = period.label === chartLabelRight.value;
            if (currentEntryMatchesUntil) {
                matchedUntilLabel = false;
            }

            const currentEntryInLabelRange = matchedFromLabel && matchedUntilLabel;
            return currentEntryInLabelRange;
        });


        const labels = filteredPeriodData.map(d => d.label);
        const allTags = [...new Set(entries.value.flatMap(entry => entry.tags || []))].sort();
        const datasets: ChartDataset[] = allTags.map(legendTitle => ({
            label: legendTitle,
            data: filteredPeriodData.map(period => period.totals[legendTitle] || 0),
            borderWidth: 2,
        }));

        // set default selection
        // if (needsLabelUpdate) {
        //     if (timePeriod === 'week' && !chartLabelLeft.value) {
        //         chartLabelLeft.value = format(add(new Date(), { weeks: -10 }), timePeriods[timePeriod].labelFormat);
        //     } else if (timePeriod === 'month' && !chartLabelLeft.value) {
        //         chartLabelLeft.value = format(add(new Date(), { months: -12 }), timePeriods[timePeriod].labelFormat);
        //     } else {
        //         chartLabelLeft.value = labels[0] || '';
        //     }

        //     chartLabelRight.value = labels[labels.length - 1] || '';
        //     totalColumnCount.value = labels.length;
        // }

        const config: ChartConfiguration = {
            type: 'bar',
            data: { datasets, labels },
            plugins: [stackedSumPlugin],
            options: {
                responsive: true,
                maintainAspectRatio: false,
                animation: {
                    onComplete: () => {
                        isDrawing.value = false; // Chart is done drawing and animating
                        console.debug('Chart drawing complete');
                    }
                },
                plugins: {
                    title: {
                        display: false,
                    },
                    legend: {
                        position: 'top',
                    }
                },
                scales: {
                    x: {
                        stacked: true,
                    },
                    y: {
                        stacked: true,
                        title: {
                            display: true,
                            text: 'Betrag (€)'
                        }
                    }
                }
            }
        };

        try {
            if (totalChart.value) {
                totalChart.value!.destroy();
                totalChart.value = null;
            }

            const ctx = totalChartCanvas.value.getContext('2d')!;
            if (ctx) {
                totalChart.value = new Chart(ctx, config);
            }
        }
        catch (error) {
            console.error('Error creating chart:', error);
            isDrawing.value = false;
        } finally {
            // isDrawing.value = false;
        }
    };

    onMounted(() => setTimeout(() => updateCharts(), 500));
    // watch(entries, () => updateCharts());
</script>
<style scoped>
    .chart-container {
        height: 300px;
        margin-bottom: 24px;
    }

    .chart-title {
        font-size: 1.2rem;
        font-weight: 500;
        margin-bottom: 16px;
        color: var(--primary);
    }

    .dark-mode .chart-title {
        color: var(--secondary);
    }

    .v-select>*>* {
        padding-bottom: 5px;
    }
</style>