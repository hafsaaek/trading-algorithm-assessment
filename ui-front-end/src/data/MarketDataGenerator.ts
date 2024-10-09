import { Table } from "@vuu-ui/vuu-data-test";
import { VuuRange } from "@vuu-ui/vuu-protocol-types";
import { generateMarketDepth } from "./data-utils";

// this is all part of the testDataProvider

const UPDATE_FREQUENCY = 250; // data is generated every 1/4 of a second 

interface UpdateGenerator {
  setTable: (table: Table) => void;
  setRange: (range: VuuRange) => void;
}

export class MarketDataGenerator implements UpdateGenerator {
  private table: Table | undefined;
  private range: VuuRange | undefined;
  private updating = false;

  setRange(range: VuuRange) {
    this.range = range;
    if (!this.updating && this.table) {
      this.startUpdating();
    }
  }

  setTable(table: Table) {
    this.table = table;
  }

  private startUpdating() {
    this.updating = true;
    this.update();
  }

  update = () => {
    if (this.range && this.table) {
      const updatedMarketData = generateMarketDepth("VOD.L");
      for (const updatedRow of updatedMarketData) {
        this.table.updateRow(updatedRow);
      }
    }

    if (this.updating) {
      window.setTimeout(this.update, UPDATE_FREQUENCY);
    }
  };
}
