/*
* Copyright 2019-2020 by Security and Safety Things GmbH
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
import Vue from 'vue'
import Vuetify from 'vuetify/lib'
/**
 * Needed for any text that is put on any of the web pages
 */
import 'roboto-fontface/css/roboto/roboto-fontface.css';
/**
 * Needed for including material icons as a local dependency
 */
import 'material-design-icons-iconfont/dist/material-design-icons.css';
Vue.use(Vuetify)
/**
 * Sets up Vue to use Vuetify
 */
export default new Vuetify({
  theme: {
    themes: {
      light: {
        primary: '#ff6a00',
        secondary: '#FFEE58',
        accent: '#FF7043',
        error: '#ff3c00',
        warning: '#795548',
        info: '#1565C0',
        success: '#4caf50'
      },
      dark: {
        primary: '#FFB74D',
        secondary: '#FFEE58',
        accent: '#FF7043',
        error: '#BF360C',
        warning: '#795548',
        info: '#1565C0',
        success: '#4caf50'
      }
    }
  },
  icons: {
    iconfont: 'md'
  }
})
