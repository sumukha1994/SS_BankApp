export default {
  TOGGLE_LOADING (state) {
    state.callingAPI = !state.callingAPI
  },
  TOGGLE_SEARCHING (state) {
    state.searching = (state.searching === '') ? 'loading' : ''
  },
  SET_USER (state, user) {
    state.user = user
  },
  SET_MODULE_MAP (state, moduleMap) {
    state.moduleMap = moduleMap
  },
  SET_TOKEN (state, token) {
    state.token = token
  }
}
