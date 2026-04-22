import { createVuetify } from 'vuetify'
import { aliases, mdi } from 'vuetify/iconsets/mdi'
import '@mdi/font/css/materialdesignicons.css'
import { VDateInput } from 'vuetify/labs/VDateInput';
import 'vuetify/styles/main.css'

export default createVuetify({
  components: {
    VDateInput,
  },
  icons: {
    defaultSet: 'mdi',
    aliases,
    sets: {
      mdi
    }
  },
  theme: {
    defaultTheme: 'light',
    cspNonce: 'dQw4w9WgXcQ',
  }
})
