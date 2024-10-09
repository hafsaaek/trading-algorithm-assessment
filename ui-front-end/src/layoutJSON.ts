import { registerComponent } from "@vuu-ui/vuu-utils";
import { MarketDepthFeature } from "./components/market-depth";
import { PricesTable } from "./components/prices-table";
import { Flexbox } from "@vuu-ui/vuu-layout";

registerComponent("MarketDepthFeature", MarketDepthFeature, "view");
registerComponent("PricesTable", PricesTable, "view");
registerComponent("Flexbox", Flexbox, "container");

export const layoutJSON = {
  type: "Flexbox", // vertical component of 3 chilren, the 1st table, your table
  props: {
    splitterSize: false,
    style: {
      flexDirection: "column",
    },
  },
  children: [ // the table containting random data generator 
    {
      type: "View",
      props: {
        resizeable: true,
        style: {
          // flexBasis: 0,
          // flexGrow: 1,
          // flexShrink: 1,
          height: 270,
          width: "auto",
        },
      },
      children: [{ type: "PricesTable" }], // the table containting random data generator 
    },
    {
      type: "View", // not sure what this is this
      props: {
        resizeable: true,
        style: {
          flexBasis: 0,
          flexGrow: 1,
          flexShrink: 1,
          height: "auto",
          width: "auto",
        },
      },
      children: [{ type: "MarketDepthFeature" }], // your own table - could edit your table properties here too!
    },
  ],
};
