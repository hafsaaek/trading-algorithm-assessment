import { useRef, useEffect, useState } from "react"; // import React hooks

export interface PriceCellProps { 
    price: number; // expected prop (price) to pass to this component is of type number (e.g., blueprint)
}

export const PriceCell = (props: PriceCellProps) => {
    const {price} = props; // take the prop to be price using type defined int eh interface

    const previousPrice = useRef<number | null>(null); // useRef to store the previous price without re-rendering (avoid rendering the compnent when previousPrice changes)
    
    const [arrowSymbol, setArrowSymbol] = useState<string>(""); // useState to store the arrow symbol and its styling persistently
    const [arrowClass, setArrowClass] = useState<string>(""); // for styling


    // useEffect to determine the arrow direction with new changes in prices
    useEffect(() => {
        if (previousPrice.current !== null) {
            const diff = price - previousPrice.current;
            if (diff > 0) {
                setArrowSymbol("▲");  setArrowClass("arrow-up"); // Price increased
            } else if (diff < 0) {
                setArrowSymbol("▼"); setArrowClass("arrow-down"); // Price decreased
            } else {
                setArrowSymbol(""); // No change - do nothing
            }
        }
        // declare previous price here and update it everytime the useEffect 
        previousPrice.current = price;
    }, 
    
    [price]); // dependecy prop: useEffect logic is triggered when price changes

    return (
        <td>
            <span className={arrowClass}>{arrowSymbol}</span>
            <span>{price.toFixed(2)}</span> {/* display price to 2 d.p. */}
        </td>   
    );
};