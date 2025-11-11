import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Chart, ChartConfiguration, ChartType, registerables } from 'chart.js';
import { Subscription } from 'rxjs';

import { 
  PredictiveAnalyticsService, 
  PredictiveInsight, 
  LoanApprovalPrediction,
  MarketTrend,
  RiskForecast,
  PerformanceMetrics
} from '../../../core/services/predictive-analytics.service';

Chart.register(...registerables);

@Component({
  selector: 'app-predictive-analytics',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './predictive-analytics.component.html',
  styleUrls: ['./predictive-analytics.component.css']
})
export class PredictiveAnalyticsComponent implements OnInit, OnDestroy {
  @ViewChild('trendsChart', { static: false }) trendsChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('riskChart', { static: false }) riskChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('performanceChart', { static: false }) performanceChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('predictionChart', { static: false }) predictionChartRef!: ElementRef<HTMLCanvasElement>;

  // Data properties
  insights: PredictiveInsight[] = [];
  marketTrends: MarketTrend[] = [];
  riskForecast: RiskForecast | null = null;
  performanceMetrics: PerformanceMetrics[] = [];
  selectedLoanPrediction: LoanApprovalPrediction | null = null;

  // UI state
  loading = false;
  error = '';
  activeTab = 'insights';
  selectedTimeframe = '6months';
  selectedMetric = 'all';

  // Charts
  private trendsChart: Chart | null = null;
  private riskChart: Chart | null = null;
  private performanceChart: Chart | null = null;
  private predictionChart: Chart | null = null;

  // Subscriptions
  private subscriptions: Subscription[] = [];

  // Make Math available in template
  Math = Math;

  // Filter options
  timeframeOptions = [
    { value: '3months', label: '3 Months' },
    { value: '6months', label: '6 Months' },
    { value: '12months', label: '12 Months' },
    { value: '24months', label: '24 Months' }
  ];

  metricOptions = [
    { value: 'all', label: 'All Metrics' },
    { value: 'risk', label: 'Risk Analysis' },
    { value: 'performance', label: 'Performance' },
    { value: 'market', label: 'Market Trends' }
  ];

  constructor(private predictiveService: PredictiveAnalyticsService) {}

  ngOnInit(): void {
    this.loadAllData();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.destroyCharts();
  }

  // ==================== Data Loading ====================

  loadAllData(): void {
    this.loading = true;
    this.error = '';

    // Load insights
    const insightsSub = this.predictiveService.getPredictiveInsights().subscribe({
      next: (insights) => {
        this.insights = insights;
        this.loading = false;
      },
      error: (error) => {
        this.error = 'Failed to load predictive insights';
        this.loading = false;
      }
    });

    // Load market trends
    const trendsSub = this.predictiveService.getMarketTrends().subscribe({
      next: (trends) => {
        this.marketTrends = trends;
        setTimeout(() => this.createTrendsChart(), 100);
      },
      error: (error) => console.error('Failed to load market trends:', error)
    });

    // Load risk forecast
    const riskSub = this.predictiveService.getRiskForecast().subscribe({
      next: (forecast) => {
        this.riskForecast = forecast;
        setTimeout(() => this.createRiskChart(), 100);
      },
      error: (error) => console.error('Failed to load risk forecast:', error)
    });

    // Load performance metrics
    const performanceSub = this.predictiveService.getPerformanceMetrics().subscribe({
      next: (metrics) => {
        this.performanceMetrics = metrics;
        setTimeout(() => this.createPerformanceChart(), 100);
      },
      error: (error) => console.error('Failed to load performance metrics:', error)
    });

    this.subscriptions.push(insightsSub, trendsSub, riskSub, performanceSub);
  }

  loadLoanPrediction(loanId: number): void {
    const predictionSub = this.predictiveService.getLoanApprovalPrediction(loanId).subscribe({
      next: (prediction) => {
        this.selectedLoanPrediction = prediction;
        setTimeout(() => this.createPredictionChart(), 100);
      },
      error: (error) => console.error('Failed to load loan prediction:', error)
    });

    this.subscriptions.push(predictionSub);
  }

  // ==================== Chart Creation ====================

  createTrendsChart(): void {
    if (!this.trendsChartRef?.nativeElement || this.marketTrends.length === 0) return;

    const ctx = this.trendsChartRef.nativeElement.getContext('2d');
    if (!ctx) return;

    // Prepare data for multiple trend lines
    const datasets = this.marketTrends.map((trend, index) => ({
      label: trend.metric,
      data: trend.predictions.map(p => p.value),
      borderColor: this.getChartColor(index),
      backgroundColor: this.getChartColor(index, 0.1),
      fill: false,
      tension: 0.4,
      pointRadius: 4,
      pointHoverRadius: 6
    }));

    const config: ChartConfiguration<'line'> = {
      type: 'line',
      data: { 
        labels: this.marketTrends[0]?.predictions.map(p => new Date(p.date).toLocaleDateString()) || [],
        datasets 
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          title: {
            display: true,
            text: 'Market Trends Prediction',
            font: { size: 16, weight: 'bold' }
          },
          legend: {
            position: 'top'
          },
          tooltip: {
            mode: 'index',
            intersect: false,
            callbacks: {
              afterLabel: (context) => {
                const dataIndex = context.dataIndex;
                const trend = this.marketTrends[context.datasetIndex];
                const confidence = trend.predictions[dataIndex]?.confidence;
                return confidence ? `Confidence: ${confidence}%` : '';
              }
            }
          }
        },
        scales: {
          x: {
            title: {
              display: true,
              text: 'Time Period'
            }
          },
          y: {
            title: {
              display: true,
              text: 'Value'
            }
          }
        },
        interaction: {
          mode: 'nearest',
          axis: 'x',
          intersect: false
        }
      }
    };

    this.trendsChart = new Chart(ctx, config);
  }

  createRiskChart(): void {
    if (!this.riskChartRef?.nativeElement || !this.riskForecast) return;

    const ctx = this.riskChartRef.nativeElement.getContext('2d');
    if (!ctx) return;

    const config: ChartConfiguration<'doughnut'> = {
      type: 'doughnut',
      data: {
        labels: ['Credit Risk', 'Market Risk', 'Operational Risk', 'Liquidity Risk'],
        datasets: [{
          data: [
            this.riskForecast.riskCategories.creditRisk,
            this.riskForecast.riskCategories.marketRisk,
            this.riskForecast.riskCategories.operationalRisk,
            this.riskForecast.riskCategories.liquidityRisk
          ],
          backgroundColor: [
            '#ef4444', // Red
            '#f59e0b', // Orange
            '#eab308', // Yellow
            '#06b6d4'  // Cyan
          ],
          borderWidth: 2,
          borderColor: '#ffffff'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          title: {
            display: true,
            text: 'Risk Distribution Forecast',
            font: { size: 16, weight: 'bold' }
          },
          legend: {
            position: 'right'
          },
          tooltip: {
            callbacks: {
              label: (context) => {
                const label = context.label || '';
                const value = context.parsed;
                return `${label}: ${value}%`;
              }
            }
          }
        }
      }
    };

    this.riskChart = new Chart(ctx, config);
  }

  createPerformanceChart(): void {
    if (!this.performanceChartRef?.nativeElement || this.performanceMetrics.length === 0) return;

    const ctx = this.performanceChartRef.nativeElement.getContext('2d');
    if (!ctx) return;

    const config: ChartConfiguration<'bar'> = {
      type: 'bar',
      data: {
        labels: this.performanceMetrics.map(m => m.metric),
        datasets: [
          {
            label: 'Current',
            data: this.performanceMetrics.map(m => m.current),
            backgroundColor: '#3b82f6',
            borderColor: '#1d4ed8',
            borderWidth: 1
          },
          {
            label: 'Predicted',
            data: this.performanceMetrics.map(m => m.predicted),
            backgroundColor: '#10b981',
            borderColor: '#047857',
            borderWidth: 1
          },
          {
            label: 'Target',
            data: this.performanceMetrics.map(m => m.target),
            backgroundColor: '#f59e0b',
            borderColor: '#d97706',
            borderWidth: 1
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          title: {
            display: true,
            text: 'Performance Metrics Comparison',
            font: { size: 16, weight: 'bold' }
          },
          legend: {
            position: 'top'
          }
        },
        scales: {
          y: {
            beginAtZero: true,
            title: {
              display: true,
              text: 'Value'
            }
          }
        }
      }
    };

    this.performanceChart = new Chart(ctx, config);
  }

  createPredictionChart(): void {
    if (!this.predictionChartRef?.nativeElement || !this.selectedLoanPrediction) return;

    const ctx = this.predictionChartRef.nativeElement.getContext('2d');
    if (!ctx) return;

    const timeline = this.selectedLoanPrediction.timeline;

    const config: ChartConfiguration<'line'> = {
      type: 'line',
      data: {
        labels: timeline.map(t => t.stage),
        datasets: [{
          label: 'Approval Probability',
          data: timeline.map(t => t.probability),
          borderColor: '#10b981',
          backgroundColor: 'rgba(16, 185, 129, 0.1)',
          fill: true,
          tension: 0.4,
          pointRadius: 6,
          pointHoverRadius: 8,
          pointBackgroundColor: timeline.map(t => 
            t.status === 'COMPLETED' ? '#10b981' : 
            t.status === 'CURRENT' ? '#f59e0b' : '#94a3b8'
          )
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          title: {
            display: true,
            text: 'Loan Approval Probability Timeline',
            font: { size: 16, weight: 'bold' }
          },
          legend: {
            display: false
          }
        },
        scales: {
          y: {
            beginAtZero: true,
            max: 100,
            title: {
              display: true,
              text: 'Probability (%)'
            }
          }
        }
      }
    };

    this.predictionChart = new Chart(ctx, config);
  }

  // ==================== Utility Methods ====================

  getChartColor(index: number, alpha: number = 1): string {
    const colors = [
      `rgba(59, 130, 246, ${alpha})`,   // Blue
      `rgba(16, 185, 129, ${alpha})`,   // Green
      `rgba(245, 158, 11, ${alpha})`,   // Orange
      `rgba(239, 68, 68, ${alpha})`,    // Red
      `rgba(139, 92, 246, ${alpha})`,   // Purple
      `rgba(6, 182, 212, ${alpha})`     // Cyan
    ];
    return colors[index % colors.length];
  }

  getInsightIcon(category: string): string {
    const icons = {
      'RISK': 'fa-exclamation-triangle',
      'OPPORTUNITY': 'fa-chart-line',
      'TREND': 'fa-trending-up',
      'ALERT': 'fa-bell'
    };
    return icons[category as keyof typeof icons] || 'fa-info-circle';
  }

  getInsightColor(impact: string): string {
    const colors = {
      'HIGH': 'danger',
      'MEDIUM': 'warning',
      'LOW': 'info'
    };
    return colors[impact as keyof typeof colors] || 'secondary';
  }

  getConfidenceColor(confidence: number): string {
    if (confidence >= 80) return 'success';
    if (confidence >= 60) return 'warning';
    return 'danger';
  }

  getTrendIcon(trend: string): string {
    const icons = {
      'INCREASING': 'fa-arrow-up',
      'DECREASING': 'fa-arrow-down',
      'STABLE': 'fa-minus',
      'UP': 'fa-arrow-up',
      'DOWN': 'fa-arrow-down'
    };
    return icons[trend as keyof typeof icons] || 'fa-minus';
  }

  getTrendColor(trend: string): string {
    const colors = {
      'INCREASING': 'success',
      'DECREASING': 'danger',
      'STABLE': 'secondary',
      'UP': 'success',
      'DOWN': 'danger'
    };
    return colors[trend as keyof typeof colors] || 'secondary';
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(value);
  }

  formatPercentage(value: number): string {
    return `${value.toFixed(1)}%`;
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    });
  }

  getAbsoluteValue(value: number): number {
    return Math.abs(value);
  }

  // ==================== Event Handlers ====================

  onTabChange(tab: string): void {
    this.activeTab = tab;
    
    // Refresh charts when tab becomes active
    setTimeout(() => {
      if (tab === 'trends' && this.trendsChart) {
        this.trendsChart.resize();
      } else if (tab === 'risk' && this.riskChart) {
        this.riskChart.resize();
      } else if (tab === 'performance' && this.performanceChart) {
        this.performanceChart.resize();
      }
    }, 100);
  }

  onTimeframeChange(): void {
    // Reload data with new timeframe
    this.loadAllData();
  }

  onMetricChange(): void {
    // Filter data based on selected metric
    // Implementation depends on specific requirements
  }

  onLoanSelect(loanId: number): void {
    this.loadLoanPrediction(loanId);
  }

  refreshData(): void {
    this.loadAllData();
  }

  exportData(): void {
    // Export analytics data to CSV/Excel
    const data = {
      insights: this.insights,
      marketTrends: this.marketTrends,
      riskForecast: this.riskForecast,
      performanceMetrics: this.performanceMetrics
    };

    const dataStr = JSON.stringify(data, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `predictive-analytics-${new Date().toISOString().split('T')[0]}.json`;
    link.click();
    URL.revokeObjectURL(url);
  }

  private destroyCharts(): void {
    if (this.trendsChart) {
      this.trendsChart.destroy();
      this.trendsChart = null;
    }
    if (this.riskChart) {
      this.riskChart.destroy();
      this.riskChart = null;
    }
    if (this.performanceChart) {
      this.performanceChart.destroy();
      this.performanceChart = null;
    }
    if (this.predictionChart) {
      this.predictionChart.destroy();
      this.predictionChart = null;
    }
  }
}
