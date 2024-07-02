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
import axios from 'axios';

IgrDataChartInteractivityModule.register();
IgrGeographicMapModule.register();

interface DroneData {
    id: string;
    latitude: number;
    longitude: number;
    nbPeople: number;
    density: number;
}

const parseValue = (val: string, type: string): number | string | Date => {
    if (type === "long" || type === "duration" || type === "unsignedLong") {
        return parseInt(val);
    } else if (type === "double") {
        return parseFloat(val);
    } else if (type && type.match(/dateTime/)) {
        return new Date(val);
    } else {
        const escapedVal = val.match(/^"(.*)"$/);
        if (escapedVal) {
            return escapedVal[1];
        }
        return val;
    }
};

const parseAnnotatedCSV = (csvText: string): Array<Array<any>> => {
    const rows = csvText.split(/\n/);
    let result: { [key: string]: number | string | Date; }[][] = [];
    let currentGroup = 0;
    result[currentGroup] = [];
    let isPreviousAnnotationRow = true;
    let attrTypes: string[] = [];
    let columnNames: string[] = [];
    let defaultValues: string[] = [];

    for (const r of rows) {
        const row = r.trim();

        if (row.length === 0) {
            currentGroup++;
            result[currentGroup] = [];
            isPreviousAnnotationRow = true;
            attrTypes = [];
            columnNames = [];
            defaultValues = [];
            continue;
        }

        let cells = row.split(/,(?=(?:(?:[^"]*"){2})*[^"]*$)/);
        cells = cells.slice(1);

        if (row.match(/^#datatype/)) {
            attrTypes = attrTypes.concat(cells);
            isPreviousAnnotationRow = true;
        } else if (row.match(/^#group/)) {
            // ignoring this information
        } else if (row.match(/^#default/)) {
            defaultValues = defaultValues.concat(cells);
        } else if (!row.match(/^#/) && isPreviousAnnotationRow) {
            columnNames = columnNames.concat(cells);
            isPreviousAnnotationRow = false;
        } else {
            let rowObj: { [key: string]: number | string | Date; } = {};
            for (let i = 0; i < cells.length; i++) {
                const cell = cells[i];
                if (cell.length > 0) {
                    rowObj[columnNames[i]] = parseValue(cell, attrTypes[i]);
                } else {
                    rowObj[columnNames[i]] = parseValue(defaultValues[i], attrTypes[i]);
                }
            }
            result[currentGroup].push(rowObj);
        }
    }
    return result;
};


export default class MapDisplayImageryHeatTiles extends React.Component {

    public map!: IgrGeographicMap;
    public tileImagery: IgrTileGeneratorMapImagery;
    private fetchInterval: NodeJS.Timeout | null = null;
    constructor(props: any) {
        super(props);

        this.tileImagery = new IgrTileGeneratorMapImagery();
        this.onMapRef = this.onMapRef.bind(this);
        this.onDataLoaded = this.onDataLoaded.bind(this);
        this.fetchData = this.fetchData.bind(this);
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
        this.fetchData();

        this.fetchInterval = setInterval(this.fetchData, 60000);
    }

    public componentWillUnmount() {
        if (this.fetchInterval) {
            clearInterval(this.fetchInterval);
        }
    }

    private fetchData = () => {
        const query = `from(bucket: "website")
            |> range(start: -2h)
            |> filter(fn: (r) => r["_measurement"] == "drone")`;

        axios.post('http://localhost:8086/api/v2/query?org=skyguards', query, {
            headers: {
                'Content-Type': 'application/vnd.flux',
                'Accept': 'application/csv',
                'Authorization': 'Token 6cf082b94f7132a1487bc05729e7a3ec08db8b8d811bf8194508ed4b15d7c353'
            }
        })
            .then(response => {
                const parsedData = parseAnnotatedCSV(response.data);
                this.onDataLoaded(parsedData);
            })
            .catch(error => {
                console.error(error);
            });
    }


    public onDataLoaded(data: Array<Array<any>>) {
        this.map.series.clear();
        if (!Array.isArray(data)) {
            console.error("Parsed data is not an array:", data);
            return;
        }

        const latitudes: number[] = [];
        const longitudes: number[] = [];
        const density: number[] = [];
        const droneMap: { [key: string]: Partial<DroneData> } = {};

        data.forEach(group => {
            group.forEach(row => {
                const droneId = row.id;
                if (!droneMap[droneId]) {
                    droneMap[droneId] = { id: droneId };
                }

                switch (row._field) {
                    case 'lat':
                        latitudes.push(Number(row._value));
                        droneMap[droneId].latitude = Number(row._value);
                        break;
                    case 'lon':
                        longitudes.push(Number(row._value));
                        droneMap[droneId].longitude = Number(row._value);
                        break;
                    case 'nbPeople':
                        droneMap[droneId].nbPeople = Number(row._value);
                        break;
                    case 'density':
                        droneMap[droneId].density = Number(row._value);
                        density.push(Number(row._value));
                        break;
                    default:
                        break;
                }
            });
        });

        const drone = Object.values(droneMap) as DroneData[];

        // generating heat map imagery tiles
        const gen = new IgrHeatTileGenerator();
        gen.xValues = longitudes;
        gen.yValues = latitudes;
        gen.values = density;
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

        console.log(dataItem);
        const latitude = WorldUtils.toStringLat(dataItem.latitude);
        const longitude = WorldUtils.toStringLon(dataItem.longitude);
        const id = dataItem.id;
        const nbPeople = dataItem.nbPeople;
        const density = dataItem.density;

        return <div className="tooltipBox">
            < div className="tooltipTitle" > {id}</div >
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
                    <div className="tooltipLbl">NbPeople:</div>
                    <div className="tooltipVal">{nbPeople}</div>
                </div>
                <div className="tooltipRow">
                    <div className="tooltipLbl">Densite:</div>
                    <div className="tooltipVal">{density.toFixed(2)}</div>
                </div>
            </div>
        </div >
    }
}

// rendering above class to the React DOM
const root = ReactDOM.createRoot(document.getElementById('root')!);
root.render(<MapDisplayImageryHeatTiles />);