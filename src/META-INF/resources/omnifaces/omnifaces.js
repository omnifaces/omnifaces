var OmniFaces=OmniFaces||{};
OmniFaces.Highlight={addErrorClass:function(f,a,d){for(var c=0;c<f.length;c++){var b=document.getElementById(f[c]);if(!b){var e=document.getElementsByName(f[c]);if(e&&e.length){b=e[0];}}if(b){b.className+=" "+a;if(d){b.focus();d=false;}}}}};
