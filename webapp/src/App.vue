<!--
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
-->
<template>
  <v-app>
    <!-- Application toolbar that allows navigation between the live view and the settings page -->
    <v-app-bar app dense>
      <v-toolbar-title>ElderCare</v-toolbar-title>
      <v-spacer></v-spacer>
      <v-btn text to="/">Live</v-btn>

    </v-app-bar>

    <v-content>
      <v-container fluid>
        <!-- The router-view is populated with the Home.Vue component when the app starts -->
        <router-view />
      </v-container>
    </v-content>

    <v-footer app>
      <v-layout justify-center row wrap>
        <span class="px-2">Lush Technology GmbH &copy; {{new Date().getFullYear()}}</span>
      </v-layout>
    </v-footer>
  </v-app>
</template>
<!-- This file sets up the Vue project and components. -->
<script lang="ts">
import { Component, Vue } from "vue-property-decorator";

@Component({
  components: {}
})
export default class App extends Vue {}
</script>
