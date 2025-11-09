package com.rfid.desktop.chart;

import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.PieChart;
import org.knowm.xchart.PieChartBuilder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler;

import java.awt.Color;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public final class AttendanceChartFactory {

    private AttendanceChartFactory() {
    }

    public static CategoryChart buildAttendanceByCaChart(Map<Integer, Long> data) {
        CategoryChart chart = new CategoryChartBuilder()
                .width(500)
                .height(300)
                .title("Thống kê điểm danh theo ca học")
                .xAxisTitle("Ca học")
                .yAxisTitle("Số lượng điểm danh")
                .build();

        chart.getStyler().setLegendVisible(false);
        chart.getStyler().setYAxisMin(0.0);
        chart.getStyler().setPlotGridVerticalLinesVisible(false);
        chart.getStyler().setChartBackgroundColor(Color.WHITE);
        chart.getStyler().setPlotBackgroundColor(Color.WHITE);

        List<Integer> categories = List.of(1, 2, 3, 4, 5);
        List<Long> values = new ArrayList<>();
        for (Integer ca : categories) {
            values.add(data.getOrDefault(ca, 0L));
        }

        chart.addSeries("Điểm danh", categories, values)
                .setFillColor(new Color(54, 162, 235));
        return chart;
    }

    public static PieChart buildAttendanceStatusChart(Map<String, Long> data) {
        PieChart chart = new PieChartBuilder()
                .width(500)
                .height(300)
                .title("Phân bố trạng thái điểm danh")
                .build();

        chart.getStyler().setLegendVisible(true);
        chart.getStyler().setLegendPosition(Styler.LegendPosition.OutsideS);
        chart.getStyler().setPlotContentSize(0.8);
        chart.getStyler().setChartBackgroundColor(Color.WHITE);
        chart.getStyler().setPlotBackgroundColor(Color.WHITE);

        Color[] colors = new Color[]{
                new Color(40, 167, 69),
                new Color(255, 193, 7),
                new Color(96, 157, 37),
                new Color(0, 123, 255),
                new Color(253, 126, 20),
                new Color(220, 53, 69)
        };

        int index = 0;
        for (Map.Entry<String, Long> entry : data.entrySet()) {
            if (entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            chart.addSeries(entry.getKey(), entry.getValue())
                    .setFillColor(colors[index % colors.length]);
            index++;
        }

        return chart;
    }

    public static XYChart buildAttendanceByHourChart(List<Long> data) {
        XYChart chart = new XYChartBuilder()
                .width(600)
                .height(350)
                .title("Xu hướng điểm danh theo giờ")
                .xAxisTitle("Giờ")
                .yAxisTitle("Số lượng")
                .build();

        chart.getStyler().setLegendVisible(false);
        chart.getStyler().setYAxisMin(0.0);
        chart.getStyler().setMarkerSize(6);
        chart.getStyler().setChartBackgroundColor(Color.WHITE);
        chart.getStyler().setPlotBackgroundColor(Color.WHITE);

        List<Double> xData = new ArrayList<>();
        List<Double> yData = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            xData.add((double) i);
            yData.add(data.size() > i ? data.get(i).doubleValue() : 0.0);
        }

        chart.addSeries("Điểm danh", xData, yData);
        return chart;
    }

    public static List<Long> prepareHourlyData(Map<Integer, Integer> rawData) {
        List<Long> hours = new ArrayList<>();
        IntStream.range(0, 24).forEach(hour -> hours.add(rawData.getOrDefault(hour, 0).longValue()));
        return hours;
    }

    public static String formatHour(int hour) {
        return LocalTime.of(hour, 0).toString();
    }
}

