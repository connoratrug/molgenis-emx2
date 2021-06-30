import { createStore as _createStore } from "vuex";

import actions from "./actions";
import getters from "./getters";
import mutations from "./mutations";
import state from "./state";

export default _createStore({
  actions,
  getters,
  mutations,
  state,
});
