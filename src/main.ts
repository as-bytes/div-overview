// import './assets/main.css'

import { createApp } from 'vue'
// import { createPinia } from 'pinia'

import App from './components/App.vue'
//import router from './router'
import createVuetify from './plugins/vuetify'
import { createPinia } from 'pinia'
import { setDefaultOptions } from 'date-fns';
import { de } from 'date-fns/locale/de'
import VueDatePicker from '@vuepic/vue-datepicker';
import '@vuepic/vue-datepicker/dist/main.css'

const app = createApp(App)

app.use(createPinia())
app.use(createVuetify)
//app.use(router)
app.component('VueDatePicker', VueDatePicker);
app.mount('#app')

setDefaultOptions({ locale: de });