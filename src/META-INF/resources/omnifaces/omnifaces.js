var OmniFaces={};
OmniFaces.Ajax=function(){var b=[];if(typeof jsf!=="undefined"){jsf.ajax.addOnEvent(function(d){if(d.status==="success"){for(var c in b){if(b.hasOwnProperty(c)){b[c].call(null)}}b=[]}})}return{addRunOnceOnSuccess:function a(c){if(typeof c==="function"){b[b.length]=c}else{throw new Error("OmniFaces.Ajax.addRunOnceOnSuccess: The given callback is not a function.")}}}}();
OmniFaces.Highlight={addErrorClass:function(e,a,d){var f=!d;for(var c=0;c<e.length;c++){var b=document.getElementById(e[c]);if(b){b.className+=" "+a;if(!f){b.focus();f=true}}}}};
