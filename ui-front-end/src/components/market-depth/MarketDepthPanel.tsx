import { MarketDepthRow } from "./useMarketDepthData";
import "./MarketDepthPanel.css";
import {PriceCell} from './PriceCell';

// an interface to define the strcuture of the the data we want paased inthis component: data (dynamic table)
interface MarketDepthPanelProps { 
    data: MarketDepthRow[];
  }
  
  // create the component
export const MarketDepthPanel = (props: MarketDepthPanelProps) => {
    console.log({ props });
    const maxQuantity = Math.max(...props.data.map(row => Math.max(row.bidQuantity, row.offerQuantity)));
    
    return (
        <table  className="MarketDepthPanel">
            <thead> {/* Table headers - 2 rows of headers */}
                <tr> 
                    <th rowSpan={2}>Index</th> {/* Index column - merges 2 rows */}
                    <th colSpan={2}>Bid</th> {/* Bid column - merges 2 columns */}
                    <th colSpan={2}>Ask</th> {/* Ask column - also merges 2 columns */}
                </tr>
                {/* Row 2: Quantity and Price under Bid and Ask */}
                <tr>
                    <th>Quantity</th>
                    <th>Price</th>
                    <th>Price</th>
                    <th>Quantity</th>
                </tr>
            </thead>
            <tbody>  {/* Table Body */}
                {props.data.map((row, index) => (
                <tr key={index}>
                    {/* Index */}
                    <td>{index}</td>
                    {/* Bid Quantity*/}
                    <td>
                        <div className="bid-bar" style={{width: `${(row.bidQuantity / maxQuantity) * 100}%`}} >{row.bidQuantity} {/* Normalise bid quantity*/}
                        </div>
                    </td>
                    {/* Bid price*/}
                    <PriceCell price={row.bid} />
                    {/* Ask price*/}
                    <PriceCell price={row.offer} />
                    {/* Ask Quantity*/}
                    <td>
                        <div className="ask-bar" style={{ width: `${(row.offerQuantity / maxQuantity) * 100}%`}} > {row.offerQuantity} {/* Normalise ask quantity*/}
                        </div>
                    </td>
                </tr>
                ))}
            </tbody>
        </table>
    )
};
