package utils;

import java.util.ArrayList;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class CurvePlotter {
    static List<XYSeries> plotCurves=new ArrayList<XYSeries>();
    static List<ChartFrame> frames = new ArrayList<ChartFrame>();
    static List<XYSeriesCollection> seriesSets = new ArrayList<XYSeriesCollection>();
    
    /**
     * @param winID window unique ID; must start from 0 and increment one by one
     * @return unique ID for the curve
     */
    public static int addSerie(String name, int winID) {
        XYSeries serie = new XYSeries(name);
        plotCurves.add(serie);
        ChartFrame frame;
        if (frames.size()>winID) {
            frame = frames.get(winID);
            XYSeriesCollection objDataset = seriesSets.get(winID);
            objDataset.addSeries(serie);
        } else {
            XYSeriesCollection objDataset = new XYSeriesCollection(serie);
            JFreeChart objChart = ChartFactory.createTimeSeriesChart(
                    name,     //Chart title
                    "Nb of iterations",     //Domain axis label
                    name,         //Range axis label
                    objDataset,         //Chart Data 
                    true,             // include legend?
                    true,             // include tooltips?
                    false             // include URLs?
                    );
            //        NumberAxis x = new NumberAxis();
            //        x.setTickUnit(new NumberTickUnit(1));
            objChart.getXYPlot().getDomainAxis().setTickLabelsVisible(false);
            frame = new ChartFrame("curve", objChart);
            frames.add(frame);
            seriesSets.add(objDataset);
            frame.pack();
            frame.setVisible(true);
        }
        return plotCurves.size()-1;
    }
    
    public static void addPoint(int serieID, double x, double y) {
       plotCurves.get(serieID).add(x, y);
    }
}
