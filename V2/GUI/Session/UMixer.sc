UMixer{

	*new{ |events,rect,name|
	    ^this.sub(events, List.new,rect,name)
	}

	*sub{ |events,parentEvents,rect,name|
		var spec, maxTrack,count, color, cview,w,level,bounds, width,top,main,scroll;
		maxTrack = events.collect{ |event| event.track }.maxItem + 1;
		count = 0;
		spec = [-90,12,\db].asSpec;
		
		width = (4+(44*(events.size))).max(300);
		w = Window.new(
			"mix - level "++parentEvents.size,
			Rect(if(rect.notNil){rect.left}{100},if(rect.notNil){rect.top}{100},800,342)
		).front;
		
		if(name.isNil){name = List["Main"]};
		
		top = CompositeView(w,Rect(0,0,800,20)).background_(Color.grey(0.3)).resize_(2);
		
		if(parentEvents.size != 0){
			SmoothButton(top,Rect(0,0,32,20),0).states_([["back"]])
						.radius_(3)
						.action_({
							var eventsForParentMixer;
							w.close;
							eventsForParentMixer = parentEvents.pop;
							name.pop;
							UMixer.sub(eventsForParentMixer,parentEvents,w.bounds,name);
						});
				
		};
		
		StaticText(top,Rect(if(parentEvents.size != 0){45}{10},2,200,15))
			.string_(name.last)
			.stringColor_(Color.white);
		
		scroll = ScrollView(w.view,Rect(0,20,800,322)).resize_(5);
		main = CompositeView(scroll,Rect(0,0,width,310));
		main.decorator = FlowLayout(main.bounds);
			
		maxTrack.do{ |j|
			events.select(_.canFreeSynth).do{ |event,i|
				var cview,faders, eventsFromFolder;
				if(event.track == j){
				color = Color.rand;
				if(event.isFolder.not){
					cview = CompositeView(main,40@300);
					cview.decorator = FlowLayout(cview.bounds);
					cview.background_(Color(0.58208955223881, 0.70149253731343, 0.83582089552239, 1.0););
					cview.decorator.shift(0,24);
					EZSmoothSlider.new(cview, Rect(0,0,32,240), events.indexOf(event), spec, layout:\vert)
						.value_(event.getGain)
						.action_({ |v|				
								event.setGain(v.value);
						});
					SmoothButton(cview,32@20)
					    .states_(
					        [[ \speaker, Color.black, Color.clear ],
					        [  \speaker, Color.red, Color.clear ]] )
                        .canFocus_(false)
                        .border_(1).background_(Color.grey(0.8))
                        .value_(event.muted.binaryValue)
                        .action_({ |v|
                            event.muted_(v.value.booleanValue)
                        });
				}{
					eventsFromFolder = event.allEvents.collect{ |event| (\event: event,\oldLevel: event.getGain) };
					cview = CompositeView(main,40@300);
					cview.decorator = FlowLayout(cview.bounds);
					cview.background_(Color(0.28208955223881, 0.50149253731343, 0.23582089552239, 1.0););
					SmoothButton(cview,32@20).states_([["open"]])
						.radius_(3)
						.action_({
							w.close;
							parentEvents.add(events);
							name.add(event.name);
							UMixer.sub(event.events,parentEvents,w.bounds,name)
						});
					EZSmoothSlider.new(cview, Rect(0,0,32,240), events.indexOf(event), spec, layout:\vert)
						.value_(0)
						.action_({ |v|
							eventsFromFolder.do{ |dict|
								dict[\event].setGain(dict[\oldLevel]+v.value);
							};
						});
						SmoothButton(cview,32@20)
					    .states_(
					        [[ \speaker, Color.black, Color.clear ],
					        [  \speaker, Color.red, Color.clear ]] )
                        .canFocus_(false)
                        .border_(1).background_(Color.grey(0.8))
                        .action_({ |v|
                            eventsFromFolder.do{ |dict|
								dict[\event].muted_(v.value.booleanValue)
							};
                        });
					};

				}
			}
		}
	}
}