import { defineStore } from "pinia"
import { Snack } from "./models"
import { ref } from "vue"
import { getCfg, updCfg } from "./db"

export const useMessagesStore = defineStore('messages', () => {
    const queue = ref<Snack[]>([])

    const add = (snack: Snack) => {
        snack.text = `#${queue.value.length + 1} ${snack.text}`;
        queue.value.push(snack);
    }

    const showError = (title: string, err: unknown, timeout = 5000) => {
        console.error(title, err);
        const text = [(title.length > 0 ? [title] : []), [(err as Error).message]].flatMap(_ => _).join('\n');
        add({
            text,
            color: 'error',
            timeout,
        })
    }

    const showInfo = (text: string, timeout = 5000) => {
        add({
            text: `${text}`,
            color: 'primary',
            timeout,
        })
    }

    const showWarn = (text: string, timeout = 5000) => {
        add({
            text: `${text}`,
            color: 'warning',
            timeout,
        })
    }

    const showSuccess = (text: string, timeout = 5000) => {
        add({
            text: `${text}`,
            color: 'success',
            timeout,
        })
    }

    return { queue, add, showError, showInfo, showSuccess, showWarn }
})

export interface ConfigStoreActions {
    init(): Promise<ConfigStoreActions & ConfigStoreData>;

    getDarkMode(): boolean;
    getCollapseState(id: string): boolean;

    toggleCollapseState(id: string): Promise<boolean>;
    toggleDarkMode(): Promise<void>;
}

export interface ConfigStoreData {
    initialized: boolean;
    darkMode: boolean;
    collapseStates: Record<string, boolean>;
}

const enum ConfigStoreKey {
    DarkMode = 'darkMode',
    CollapseState = 'collapseState',
}

export const useConfigStore = async (): Promise<ConfigStoreActions & ConfigStoreData> => {
    return createConfigStore().init();
}

const createConfigStore = defineStore('configStore', {
  state: (): ConfigStoreData => ({
    darkMode: false,
    collapseStates: {},
    initialized: false,
  }),
  actions: {
    async init(): Promise<ConfigStoreActions & ConfigStoreData> {
      this.darkMode = await getCfg(ConfigStoreKey.DarkMode, false);
      this.collapseStates = await getCfg(ConfigStoreKey.CollapseState, {});
      this.initialized = true;
      return this;
    },

    getDarkMode(): boolean { return this.darkMode },
    getCollapseState(id: string): boolean { 
        console.debug('getCollapseState', this.collapseStates[id]);
        return this.collapseStates[id] ?? false;
    },
    async toggleCollapseState(id: string): Promise<boolean> {
        try {
            console.debug('toggleCollapseState before:', this.collapseStates[id]);
            this.collapseStates[id] = !this.collapseStates[id];
            console.debug('toggleCollapseState before:', this.collapseStates[id])
            updCfg(ConfigStoreKey.CollapseState, this.collapseStates);
            return this.collapseStates[id];
        } catch (error) {
            useMessagesStore().showError('Fehler beim Speichern des Collapse-Status: ', error);
        }
        return false;
    },

    async toggleDarkMode(): Promise<void> {
        try {
            this.darkMode = !this.darkMode;
            await updCfg(ConfigStoreKey.DarkMode, this.darkMode);
        } catch (error) {
            useMessagesStore().showError('Fehler beim Speichern des Dark-Mode-Status: ', error);
        }
    },

  }
})


// export const useConfigStore = defineStore<string, ConfigStore>('config', () => {
//     const darkMode = ref<boolean | null>(null);

//     const store: ConfigStore = {
//         darkMode: darkMode!,
       
//         getDarkMode(): boolean { return darkMode.value; },
//         async getDarkMode(): Promise<boolean> { 
//             return await getCfg<boolean>(getKey(ConfigKey.DarkMode, globalId)) as boolean 
//         },


//         async setDarkMode(val: boolean): Promise<boolean> {
//             await updCfg(getKey(ConfigKey.DarkMode, globalId), val);
//             return val;
//         },
//         async setCollapseState(id: string, state: boolean): Promise<boolean> {
//             return await updCfg(getKey(ConfigKey.CollapseState, id), state) as boolean;
//         },
//         async getCollapseState(id: string): Promise<boolean> {
//             return await getCfg<boolean>(getKey(ConfigKey.CollapseState, id)) ?? false;
//         }
//     };

//     return store as ConfigStore;
// })