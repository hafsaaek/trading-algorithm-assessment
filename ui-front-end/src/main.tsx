import ReactDOM from "react-dom/client";
import App from "./App.tsx";
import { TestDataProvider } from "./data/TestDataProvider.tsx";

import "@vuu-ui/vuu-icons/index.css";
import "@vuu-ui/vuu-theme/index.css";
import "./index.css";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <TestDataProvider>
    <App />
  </TestDataProvider> 
// TestDataProvider is a react context provider: in replacement of the VUUsERVER DATA - used for testing too
// PROP DRILLING: it's passing down propps through compnents and it can get complex due to the hierachy of how components are usually deisgned as they're usualy neseted.
  // a react context provider allows for a component at the bottom of the hierachy to recieve data that doesn't have to be passed in the components before it
);
