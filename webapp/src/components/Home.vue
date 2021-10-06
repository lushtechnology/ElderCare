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
<!-- The template tag contains the HTML code -->
<template>
  <v-container fluid>
    <v-row>
      <v-alert v-if="liveViewError" type="error" outlined :value="true">Unable to connect to stream!</v-alert>
      <img width="100%" height="100%" :src="liveViewUrl"/>
    </v-row>
  </v-container>
</template>
<!-- The script tag contains the Typescript code -->
<script lang='ts'>
import { Component, Vue } from 'vue-property-decorator'
import axios from 'axios'
/**
 * Maps a new vue component called home to this file
 */
@Component({
  name: 'home'
})
export default class Home extends Vue {
  /**
   * Points to the rest/example/live endpoint
   */
  liveViewUrl: string|null = null
  /**
   * If the rest/example/live endpoint fails to obtain an image, liveViewError is set to true, and the <v-alert> tag is displayed.
   */
  liveViewError: boolean = false

  /**
   * Rendering loop. Continously calls itself to display live images from the backend while the home vue component is active.
   */
  private retrieveImage () {
    const url = 'rest/example/live?time=' + Date.now()
    const img = new Image()
    img.onload = () => {
      this.liveViewUrl = url
      this.liveViewError = false

      /**
      * If the route is no longer at the home component then the rendering loop should be interupted.  
       */
      if(this.$route.name == "home") {
        window.requestAnimationFrame(this.retrieveImage)
      }  
    }
    img.onerror = () => {
      this.liveViewError = true
      /**
       * If we encounter error screen, retry in 500 ms
       */
      setTimeout(() => this.retrieveImage(), 500)
    }
    img.src = url
  }

  /**
   * Vue lifecycle hook for when this component has been mounted to the DOM. Used to start the rendering loop.
   */
  mounted() {

    /**
     * Calls the retrieveImage() function, which obtains an image from the rest/example/live endpoint, and continuously calls itself to update the image being displayed
     * in the <img> tag
     */
    this.retrieveImage()
  }

  /**
   * One of the six Vue Lifecycle components. This function attempts to obtain a response from the rest/example/test endpoint. This endpoint
   * is just used for development purposes as a simple way to verify a connection to the webserver.
   */
  created() {
    axios.get('rest/example/test').then(response => {
      if (response.data) {
        console.log(response.data)
      } else {
        console.log("no data received")
      }
    })
  }
}
</script>
<!-- Below is where css stylings go -->
<style scoped>
</style>
