var OmniFaces={};
OmniFaces.Ajax=function(){var a=[];if(typeof jsf!=="undefined"){jsf.ajax.addOnEvent(function(d){if(d.status==="success"){for(var c in a){if(a.hasOwnProperty(c)){a[c].call(null)}}a=[]}})}return{addOnload:function b(c){if(typeof c==="function"){a[a.length]=c}else{throw new Error("OmniFaces.Ajax.addOnload: The given callback is not a function.")}}}}();
OmniFaces.Highlight={addErrorClass:function(e,a,d){var f=!d;for(var c=0;c<e.length;c++){var b=document.getElementById(e[c]);if(b){b.className+=" "+a;if(!f){b.focus();f=true}}}}};
