(function(e){function t(t){for(var n,o,c=t[0],s=t[1],l=t[2],f=0,p=[];f<c.length;f++)o=c[f],Object.prototype.hasOwnProperty.call(r,o)&&r[o]&&p.push(r[o][0]),r[o]=0;for(n in s)Object.prototype.hasOwnProperty.call(s,n)&&(e[n]=s[n]);u&&u(t);while(p.length)p.shift()();return i.push.apply(i,l||[]),a()}function a(){for(var e,t=0;t<i.length;t++){for(var a=i[t],n=!0,c=1;c<a.length;c++){var s=a[c];0!==r[s]&&(n=!1)}n&&(i.splice(t--,1),e=o(o.s=a[0]))}return e}var n={},r={app:0},i=[];function o(t){if(n[t])return n[t].exports;var a=n[t]={i:t,l:!1,exports:{}};return e[t].call(a.exports,a,a.exports,o),a.l=!0,a.exports}o.m=e,o.c=n,o.d=function(e,t,a){o.o(e,t)||Object.defineProperty(e,t,{enumerable:!0,get:a})},o.r=function(e){"undefined"!==typeof Symbol&&Symbol.toStringTag&&Object.defineProperty(e,Symbol.toStringTag,{value:"Module"}),Object.defineProperty(e,"__esModule",{value:!0})},o.t=function(e,t){if(1&t&&(e=o(e)),8&t)return e;if(4&t&&"object"===typeof e&&e&&e.__esModule)return e;var a=Object.create(null);if(o.r(a),Object.defineProperty(a,"default",{enumerable:!0,value:e}),2&t&&"string"!=typeof e)for(var n in e)o.d(a,n,function(t){return e[t]}.bind(null,n));return a},o.n=function(e){var t=e&&e.__esModule?function(){return e["default"]}:function(){return e};return o.d(t,"a",t),t},o.o=function(e,t){return Object.prototype.hasOwnProperty.call(e,t)},o.p="/app/com.securityandsafetythings.examples.tflitedetector/";var c=window["webpackJsonp"]=window["webpackJsonp"]||[],s=c.push.bind(c);c.push=t,c=c.slice();for(var l=0;l<c.length;l++)t(c[l]);var u=s;i.push([0,"chunk-vendors"]),a()})({0:function(e,t,a){e.exports=a("cd49")},cd49:function(e,t,a){"use strict";a.r(t);a("e260"),a("e6cf"),a("cca6"),a("a79d");var n=a("2b0e"),r=a("f309");a("d5e8"),a("d1e78");n["a"].use(r["a"]);var i=new r["a"]({theme:{themes:{light:{primary:"#ff6a00",secondary:"#FFEE58",accent:"#FF7043",error:"#ff3c00",warning:"#795548",info:"#1565C0",success:"#4caf50"},dark:{primary:"#FFB74D",secondary:"#FFEE58",accent:"#FF7043",error:"#BF360C",warning:"#795548",info:"#1565C0",success:"#4caf50"}}},icons:{iconfont:"md"}}),o=function(){var e=this,t=e.$createElement,a=e._self._c||t;return a("v-app",[a("v-app-bar",{attrs:{app:"",dense:""}},[a("v-toolbar-title",[e._v("TFliteDetector Example")]),a("v-spacer"),a("v-btn",{attrs:{text:"",to:"/"}},[e._v("Live")]),a("v-btn",{attrs:{to:"/settings",text:""}},[e._v("Settings")])],1),a("v-content",[a("v-container",{attrs:{fluid:""}},[a("router-view")],1)],1),a("v-footer",{attrs:{app:""}},[a("v-layout",{attrs:{"justify-center":"",row:"",wrap:""}},[a("span",{staticClass:"px-2"},[e._v("Security and Safety Things © "+e._s((new Date).getFullYear()))])])],1)],1)},c=[],s=a("d4ec"),l=a("262e"),u=a("2caf"),f=a("9ab4"),p=a("60a3"),v=function(e){Object(l["a"])(a,e);var t=Object(u["a"])(a);function a(){return Object(s["a"])(this,a),t.apply(this,arguments)}return a}(p["b"]);v=Object(f["a"])([Object(p["a"])({components:{}})],v);var d=v,b=d,m=a("2877"),h=a("6544"),y=a.n(h),g=a("7496"),w=a("40dc"),j=a("8336"),x=a("a523"),O=a("a75b"),k=a("553a"),V=a("a722"),_=a("2fa4"),S=a("2a7f"),C=Object(m["a"])(b,o,c,!1,null,null,null),F=C.exports;y()(C,{VApp:g["a"],VAppBar:w["a"],VBtn:j["a"],VContainer:x["a"],VContent:O["a"],VFooter:k["a"],VLayout:V["a"],VSpacer:_["a"],VToolbarTitle:S["a"]});var T=a("8c4f"),E=function(){var e=this,t=e.$createElement,a=e._self._c||t;return a("v-container",{attrs:{fluid:""}},[a("v-row",[e.liveViewError?a("v-alert",{attrs:{type:"error",outlined:"",value:!0}},[e._v("Unable to connect to stream!")]):e._e(),a("img",{attrs:{width:"100%",height:"100%",src:e.liveViewUrl}})],1)],1)},B=[],P=(a("b0c0"),a("bee2")),I=a("bc3a"),U=a.n(I),$=function(e){Object(l["a"])(a,e);var t=Object(u["a"])(a);function a(){var e;return Object(s["a"])(this,a),e=t.apply(this,arguments),e.liveViewUrl=null,e.liveViewError=!1,e}return Object(P["a"])(a,[{key:"retrieveImage",value:function(){var e=this,t="rest/example/live?time="+Date.now(),a=new Image;a.onload=function(){e.liveViewUrl=t,e.liveViewError=!1,"home"==e.$route.name&&window.requestAnimationFrame(e.retrieveImage)},a.onerror=function(){e.liveViewError=!0,setTimeout((function(){return e.retrieveImage()}),500)},a.src=t}},{key:"mounted",value:function(){this.retrieveImage()}},{key:"created",value:function(){U.a.get("rest/example/test").then((function(e){e.data?console.log(e.data):console.log("no data received")}))}}]),a}(p["b"]);$=Object(f["a"])([Object(p["a"])({name:"home"})],$);var A=$,D=A,M=a("0798"),J=a("0fd9"),L=Object(m["a"])(D,E,B,!1,null,"fbf7647c",null),R=L.exports;y()(L,{VAlert:M["a"],VContainer:x["a"],VRow:J["a"]});var q=function(){var e=this,t=e.$createElement,a=e._self._c||t;return a("v-container",[a("v-row",{attrs:{align:"center",justify:"center"}},[a("v-header",{attrs:{align:"center",justify:"center"}},[e._v("Confidence threshold")])],1),a("v-row",{staticClass:"my-3 py-3",attrs:{align:"center",justify:"center"}},[a("v-col",{attrs:{cols:"4",sm:"3",md:"3",lg:"3",xl:"3"}},[a("v-slider",{staticClass:"ma-0 ml-1",attrs:{min:e.min,max:e.max,step:this.step,"hide-details":"",value:e.slider,"thumb-label":"always"},model:{value:e.slider,callback:function(t){e.slider=t},expression:"slider"}})],1)],1),a("v-row",{attrs:{justify:"center"}},[a("div",{staticClass:"my-2"},[a("v-btn",{attrs:{large:"",color:"primary"},on:{click:function(t){return e.postSettings()}}},[e._v("Submit")])],1)]),a("div",{staticClass:"text-center ma-2"},[a("v-snackbar",{model:{value:e.snackbar,callback:function(t){e.snackbar=t},expression:"snackbar"}},[e._v(" "+e._s(e.snackBarText)+" "),a("v-btn",{attrs:{color:"orange",snackBarText:""},on:{click:function(t){e.snackbar=!1}}},[e._v("Close")])],1)],1)],1)},Y=[],z=function(e){Object(l["a"])(a,e);var t=Object(u["a"])(a);function a(){var e;return Object(s["a"])(this,a),e=t.apply(this,arguments),e.min=0,e.max=1,e.slider=0,e.step=.01,e.snackbar=!1,e.snackBarText="",e}return Object(P["a"])(a,[{key:"mounted",value:function(){this.getSettings()}},{key:"postSettings",value:function(){var e={headers:{"Content-Type":"application/json"}},t={confidence:this.slider},a=this;U.a.post("rest/example/settings",t,e).then((function(e){a.snackbar=!0,a.snackBarText="Settings Updated"})).catch((function(e){a.snackbar=!0,a.snackBarText="Settings update failed"}))}},{key:"getSettings",value:function(){var e=this,t=this;U.a.get("rest/example/settings").then((function(t){if(t.data){var a=t.data;e.slider=a.confidence}})).catch((function(e){t.snackbar=!0,t.snackBarText="Settings sync failed"}))}}]),a}(p["b"]);z=Object(f["a"])([Object(p["a"])({name:"Settings"})],z);var G=z,H=G,K=a("62ad"),N=a("ba0d"),Q=a("2db4"),W=Object(m["a"])(H,q,Y,!1,null,"c203c760",null),X=W.exports;y()(W,{VBtn:j["a"],VCol:K["a"],VContainer:x["a"],VRow:J["a"],VSlider:N["a"],VSnackbar:Q["a"]}),n["a"].use(T["a"]);var Z=new T["a"]({mode:"history",base:"/app/com.securityandsafetythings.examples.tflitedetector/",routes:[{path:"/",name:"home",component:R},{path:"/settings",name:"settings",component:X},{path:"*",redirect:"/"}]});n["a"].config.productionTip=!1,new n["a"]({router:Z,vuetify:i,render:function(e){return e(F)}}).$mount("#app")}});
//# sourceMappingURL=app.bd196c26.js.map