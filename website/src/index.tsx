import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import { IgrGeographicMapModule } from 'igniteui-react-maps';
import { IgrGeographicMap } from 'igniteui-react-maps';
import { IgrGeographicTileSeries } from 'igniteui-react-maps';
import { IgrGeographicHighDensityScatterSeries } from 'igniteui-react-maps';
import { IgrDataChartInteractivityModule } from 'igniteui-react-charts';
import { IgrHeatTileGenerator } from 'igniteui-react-core';
import { IgrTileGeneratorMapImagery } from 'igniteui-react-maps';
import { IgrDataContext } from 'igniteui-react-core';

import WorldUtils from "./WorldUtils"
// background worker
import Worker from 'worker-loader!./heatworker.worker.ts';

IgrDataChartInteractivityModule.register();
IgrGeographicMapModule.register();

export default class MapDisplayImageryHeatTiles extends React.Component {

    public map!: IgrGeographicMap;
    public tileImagery: IgrTileGeneratorMapImagery;

    constructor(props: any) {
        super(props);

        this.tileImagery = new IgrTileGeneratorMapImagery();
        this.onMapRef = this.onMapRef.bind(this);
        this.onDataLoaded = this.onDataLoaded.bind(this);
    }

    public render(): JSX.Element {
        return (
            <div className="container sample">
                <div className="container" >
                    <IgrGeographicMap
                        ref={this.onMapRef}
                        width="100%"
                        height="100%"
                        zoomable={true} />
                </div>
                <div className="overlay-bottom-right overlay-border">Imagery Tiles: @OpenStreetMap</div>
            </div>
        );
    }

    public onMapRef(geoMap: IgrGeographicMap) {
        if (!geoMap) { return; }

        this.map = geoMap;
        // Set initial view to France using window rect properties
        this.map.windowRect = { left: 0.49, top: 0.334, width: 0.025, height: 0.036 };

    }

    public componentDidMount() {
        const data = `
            DroneID,Lat,Lon,PopulationDensity,Speed,Battery
            Drone1,48.8566,2.3522,0.1,10,29
            Drone2,48.8570,2.3540,0.19,20,30
            Drone3,48.8580,2.3550,0.18,30,31
            Drone4,48.8590,2.3560,0.17,40,32
            Drone5,48.8600,2.3570,0.16,50,33
            Drone6,48.8610,2.3580,0.15,60,34
            Drone7,48.8620,2.3590,0.14,70,35
            Drone8,48.8647,2.3490,0.20,15,50
            Drone9,48.8670,2.3318,0.18,18,55
            Drone10,48.8700,2.3294,0.17,12,53
            Drone11,48.8738,2.2950,0.18,20,60
            Drone12,48.8584,2.2945,0.16,22,45
            Drone13,48.8606,2.3376,0.15,25,48
            Drone14,48.8529,2.3470,0.14,28,49
            Drone15,48.8614,2.3447,0.14,30,52
            Drone16,48.8668,2.3350,0.15,35,47
            Drone17,48.8721,2.3430,0.14,40,46
            Drone18,48.8757,2.3456,0.13,45,44
            Drone19,48.8512,2.3696,0.13,50,42
            Drone20,48.8358,2.3621,1,55,41`.trim();

        this.onDataLoaded(data)
    }

    public onDataLoaded(csvData: string) {
        const csvLines = csvData.split("\n");

        const latitudes: number[] = [];
        const longitudes: number[] = [];
        const populations: number[] = [];
        const drone: any[] = [];

        // parsing CSV data and creating geographic locations
        for (let i = 1; i < csvLines.length; i++) {
            const columns = csvLines[i].split(",");
            latitudes.push(Number(columns[1]));
            longitudes.push(Number(columns[2]));
            populations.push(Number(columns[3]));
            drone.push({
                id: columns[0],
                latitude: Number(columns[1]),
                longitude: Number(columns[2]),
                battery: Number(columns[4]),
                speed: Number(columns[5])
            });
        }

        // generating heat map imagery tiles
        const gen = new IgrHeatTileGenerator();
        gen.xValues = longitudes;
        gen.yValues = latitudes;
        gen.values = populations;
        gen.blurRadius = 10;
        gen.maxBlurRadius = 20;
        gen.useBlurRadiusAdjustedForZoom = true;
        gen.minimumColor = "rgba(100, 255, 0, 0.5)";
        gen.maximumColor = "rgba(255, 255, 0, 0.5)";
        gen.useGlobalMinMax = true;
        gen.useGlobalMinMaxAdjustedForZoom = true;
        gen.useLogarithmicScale = true;
        gen.useWebWorkers = true;
        gen.webWorkerInstance = new Worker();
        gen.scaleColors = [
            "rgba(0, 0, 255, .251)", "rgba(0, 255, 255, .3765)",
            "rgba(50,205,50, .2675)", "rgba(255, 255, 0, .7059)",
            "rgba(255, 0, 0, .7843)"
        ];
        this.tileImagery.tileGenerator = gen;

        // generating heat map series
        const series = new IgrGeographicTileSeries({ name: "heatMapSeries" });
        series.tileImagery = this.tileImagery;
        series.showDefaultTooltip = true;

        // add heat map series to the map
        this.map.series.add(series);

        // creating HD series with loaded data
        const geoSeries = new IgrGeographicHighDensityScatterSeries({ name: "hdSeries" });
        geoSeries.dataSource = drone;
        geoSeries.longitudeMemberPath = "longitude";
        geoSeries.latitudeMemberPath = "latitude";
        geoSeries.heatMaximumColor = "rgba(0, 0, 0, 0)";
        geoSeries.heatMinimumColor = "rgba(0, 0, 0, 0)";
        geoSeries.heatMinimum = 0;
        geoSeries.heatMaximum = 5;
        geoSeries.pointExtent = 2;
        geoSeries.tooltipTemplate = this.createTooltip;
        geoSeries.mouseOverEnabled = true;

        // adding HD series to the geographic map
        this.map.series.add(geoSeries);
    }

    public createTooltip(context: any) {
        const dataContext = context.dataContext as IgrDataContext;

        if (!dataContext) return null;

        const dataItem = dataContext.item as any;
        if (!dataItem) return null;

        const latitude = WorldUtils.toStringLat(dataItem.latitude);
        const longitude = WorldUtils.toStringLon(dataItem.longitude);
        const id = dataItem.id;
        const battery = dataItem.battery;
        const speed = dataItem.speed;

        return <div className="tooltipBox">
            <div className="tooltipTitle">{id}</div>
            <div>
                <div className="tooltipRow">
                    <div className="tooltipLbl">Latitude:</div>
                    <div className="tooltipVal">{latitude}</div>
                </div>
                <div className="tooltipRow">
                    <div className="tooltipLbl">Longitude:</div>
                    <div className="tooltipVal">{longitude}</div>
                </div>
                <div className="tooltipRow">
                    <div className="tooltipLbl">Battery:</div>
                    <div className="tooltipVal">{battery}</div>
                </div>
                <div className="tooltipRow">
                    <div className="tooltipLbl">Speed:</div>
                    <div className="tooltipVal">{speed}</div>
                </div>
            </div>
        </div>
    }
}

// rendering above class to the React DOM
const root = ReactDOM.createRoot(document.getElementById('root')!);
root.render(<MapDisplayImageryHeatTiles />);