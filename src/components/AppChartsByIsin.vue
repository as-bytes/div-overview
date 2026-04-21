<template>
    <v-card class="my-6" :elevation="10">
        <v-card-title>
            Diagramme
            <span class="chart-title font-11">
                - Kumulative Dividenden pro ISIN
            </span>
            <div class="float-right">
                <v-progress-circular v-if="isDrawing" indeterminate size="26" color="primary" />
                <Suspense>
                    <AppCardToggle v-model="hideContent" id="Kum-Div-Pro-Isin" />
                </Suspense>
            </div>
        </v-card-title>
        <v-card-text class="chart-container" v-show="!configStore.getCollapseState('Kum-Div-Pro-Isin')">
            <canvas ref="totalChartCanvas"></canvas>
        </v-card-text>
    </v-card>
</template>

<script
    setup
    lang="ts"
>
    import { ChartPeriodData, Dividend } from '../models';
    import { onMounted, ref } from 'vue';
    import { Chart, ChartConfiguration, ChartDataset, LegendItem, registerables } from 'chart.js';
    import { stackedSumPlugin } from '../utils';
    import { useConfigStore } from '../stores';
    import AppCardToggle from './incl/AppCardToggle.vue';

    // todo tree shake?
    Chart.register(...registerables);

    const entries = defineModel<Dividend[]>({ default: () => [] });

    const totalChartCanvas = ref<HTMLCanvasElement | null>(null);
    const totalChart = ref<Chart | null>(null);

    const isDrawing = ref(false);
    const hideContent = ref(false);

    const configStore = await useConfigStore();

    const updateCharts = async () => await updateTotalChart();
    const sortDatesAscending = (a: Dividend, b: Dividend) => a.date.getTime() - b.date.getTime();

    const addEntryToPeriod = (periodData: Record<string, ChartPeriodData>, periodKey: string, label: string, entry: Dividend): void => {
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

    const updateTotalChart = async () => {
        if (!totalChartCanvas.value || isDrawing.value) return;
        isDrawing.value = true;

        const periodData: Record<string, ChartPeriodData> = {};

        entries.value.slice().sort(sortDatesAscending).forEach(entry => {
            const date = new Date(entry.date);
            const mapKey = [date.getFullYear(), entry.isin].join('-');
            const label = entry.isin;
            addEntryToPeriod(periodData, mapKey, label, entry);
        });

        // const labels = Object.values(periodData).map(d => d.label);
        const allIsins = [...new Set(entries.value.flatMap(entry => entry.isin || []))].sort();
        // const datasets: ChartDataset[] = allIsins.map(legendTitle => ({
        //     label: legendTitle,
        //     data: Object.values(periodData).map(period => period.totals[legendTitle] || 0),
        //     borderWidth: 2,
        // }));

        const maxYearsBack = 4;

        const years = new Array(maxYearsBack).fill(0).map((_, index) => (new Date().getFullYear() - maxYearsBack + index + 1));


        const sumByIsin: Map<string, number> = new Map();
        allIsins.forEach(isin => {
            sumByIsin.set(isin, entries.value
                .filter(entry => entry.isin === isin)
                .reduce((sum, entry) => sum + entry.amount, 0)
            );
        });

        const sortedIsins = Array.from(sumByIsin.entries())
            .sort((a, b) => b[1] - a[1])
            .map(([isin]) => isin);
        const isins = sortedIsins.slice(0, 15);


        const yearTotals: Record<string, number> = {}
        const datasets: ChartDataset[] = years.map((year) => {
            const sumByIsin = isins.map(isin => entries.value
                .filter(entry => entry.isin === isin && entry.date.getFullYear() === year)
                .reduce((sum, entry) => sum + entry.amount, 0));
            const sumByYear = entries.value
                .filter(entry => entry.date.getFullYear() === year)
                .reduce((sum, entry) => sum + entry.amount, 0);

            yearTotals[year] = sumByYear;

            return {
                label: `${year}`,
                data: sumByIsin,
                borderWidth: 2,
            }
        });


        const labels: Array<string[]> = [];
        for (const isin of isins) {
            labels.push([isin]); // await getNameForIsin(isin)
        }

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
                        display: true,
                        position: 'top',
                        labels: {
                            boxHeight: 20,
                            generateLabels: function (chart): Array<LegendItem & { text: string[] }> {
                                // Default labels generator, but with custom text added
                                return chart.data.datasets.map((dataset, i) => {
                                    const base = Chart.defaults.plugins.legend.labels.generateLabels(chart)[i] ?? '';
                                    return {
                                        ...base,
                                        text: [dataset.label ?? 'n/v', `(${yearTotals[dataset.label!]!.toLocaleString('de-DE', {
                                            minimumFractionDigits: 0,
                                            maximumFractionDigits: 0
                                        })} €)`],
                                    } as LegendItem & { text: string[] };
                                });
                            }
                        }
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