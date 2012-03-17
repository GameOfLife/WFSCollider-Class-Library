


(
x = UScore(
	UEvent(UChain(U(\blip,[\rate,2])),0,0,4,2,2),
	UEvent(UChain(U(\blip,[\rate,0.5])),0,2,4,2,2)
);
x.play(s)
)



x = USession(
	[UChain(U(\blip,[\rate,2]))],
[UScore(
	UEvent(UChain(U(\blip,[\rate,2])),0,0,4,2,2),
	UEvent(UChain(U(\blip,[\rate,0.5])),0,2,4,2,2)
)]
);

x.startTracks(s)
x.startAll(s)


5 collect _+1

5.collect{ |x| x+1 }

x.tracks.collect{ |x| x <| U(\output) }