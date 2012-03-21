var OmniFaces={};
OmniFaces.Highlight={addErrorClass:function(e,a,d){var f=!d;for(var c=0;c<e.length;c++){var b=document.getElementById(e[c]);if(b){b.className+=" "+a;if(!f){b.focus();f=true}}}}};