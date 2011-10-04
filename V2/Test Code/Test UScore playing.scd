//play 3 seconds and stop by itself
(
r = UChain(0,0,3,false,\sine, \output).fadeOut_(1).fadeIn_(1);
z = UScore(r);
x = UScoreEditor(z);
y = UScoreEditorGUI(x);
fork{
	z.start(UServerCenter.servers,0);
}
)


//play 3 seconds and stop;
(
r = UChain(0,0,10,false,\sine, \output).fadeOut_(1).fadeIn_(1);
z = UScore(r);
x = UScoreEditor(z);
y = UScoreEditorGUI(x);
fork{
	z.start(UServerCenter.servers,0);
	0.5.wait;
	if(z.isPlaying != true){ "error ! isPlaying should be true".throw };
	3.wait;
	z.stop;
	if(z.isPlaying != false){ "error ! isPlaying should be false".throw };
}
)

//play 3 seconds and pause for 1 seconds then stop;
(
r = UChain(0,0,10,false,\sine, \output).fadeOut_(1).fadeIn_(1);
z = UScore(r);
x = UScoreEditor(z);
y = UScoreEditorGUI(x);
fork{
	z.start(UServerCenter.servers,0);
	3.wait;
	z.pause;
	if(z.isPaused != true){ "error ! isPaused should be true".throw };
	1.wait;
	z.stop;
}
)

//should play 2 tones, the lower tone stop after 5 seconds, the hight tone stops after 8 seconds
(
r = UChain(0,0,5,false,\sine, \output).fadeOut_(1).fadeIn_(1);
r.units[0].set(\freq,956);
v = UChain(0,1,5,true,\sine, \output).fadeOut_(1).fadeIn_(1);
v.units[0].set(\freq,400);
z = UScore(r,v);
x = UScoreEditor(z);
y = UScoreEditorGUI(x);
fork{
	z.start(UServerCenter.servers,0);
	3.wait;
	z.pause;
	if(z.isPaused != true){ "error ! isPaused should be true".throw };
	3.wait;
	z.resume;
	if(z.isPaused != false){ "error ! isPaused should be true".throw };
}
)



