var OmniFaces=OmniFaces||{};
OmniFaces.Ajax=function(){var b=[];var c=function c(e){if(e.status==="success"){for(var d in b){if(b.hasOwnProperty(d)){b[d].call(null)}}b=[]}};return{addRunOnceOnSuccess:function a(d){if(typeof d==="function"){if(!b.length){jsf.ajax.addOnEvent(c)}b[b.length]=d}else{throw new Error("OmniFaces.Ajax.addRunOnceOnSuccess: The given callback is not a function.")}}}}();
OmniFaces.Highlight={addErrorClass:function(e,a,d){var f=!d;for(var c=0;c<e.length;c++){var b=document.getElementById(e[c]);if(b){b.className+=" "+a;if(!f){b.focus();f=true}}}}};
