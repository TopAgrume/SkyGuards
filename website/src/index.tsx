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
            DroneID,Lat,Lon,Population,Battery,Speed
            Drone1,48.8566,2.3522,19354922,10,29
            Drone2,48.8570,2.3540,12815475,20,30
            Drone3,48.8580,2.3550,8675982,30,31
            Drone4,48.8590,2.3560,6381966,40,32
            Drone5,48.8600,2.3570,5733259,50,33
            Drone6,48.8610,2.3580,5637884,60,34
            Drone7,48.8620,2.3590,5446468,70,35
            `.trim();

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
        gen.blurRadius = 6;
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
        geoSeries.pointExtent = 1;
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

        console.log(dataItem)

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
