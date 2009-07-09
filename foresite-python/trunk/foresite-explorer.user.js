// ==UserScript==
// @name           Foresite ExplOREr: ORE Visualization
// @namespace      http://foresite.cheshire3.org/greasemonkey/v1.1/
// @include        http://www.jstor.org/stable/*
// @include        http://www.jstor.org.ezproxy.liv.ac.uk/stable/*
// @include        http://www.flickr.com/photos/*
// @include        http://www.flickr.com/search/*
// @include        http://www.flickr.com/groups/*
// @include        http://www.amazon.com/gp/registry/wishlist/*
// @include        http://groupme.org/GroupMe/group/*
// @include        http://www.myexperiment.org/packs/*
// @include        http://*
// ==/UserScript==

// optimize checking for * here, before loading everything else

var links = document.getElementsByTagName('link');
var here = location.href;
var remURI = '';
var remFormat = '';
var tempAggrURI = '';
var debug = 0;

for (var i = 0 ; i < links.length ; i++) {
  var link = links[i];
  var rel = link.getAttribute('rel');
  if (rel == 'resourcemap') {
    remURI = link.getAttribute('href');
    remFormat = link.getAttribute('type');

    if (remFormat == 'application/atom+xml') {
      remURI = 'http://foresite.cheshire3.org/txr/' + remURI;
    }
    break;
  }
 }
if (remURI == '' && here.indexOf('www.jstor.org') == -1 
    && here.indexOf('www.flickr.com') == -1
    && here.indexOf('www.amazon.com') == -1
    && here.indexOf('www.myexperiment.org') == -1
    && here.indexOf('groupme.org/GroupMe/') == -1) {
  return;
 }


// global vars

function none() {};
var SVG_NS = "http://www.w3.org/2000/svg";
var XHTML_NS = "http://www.w3.org/1999/xhtml";
var RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

var ORE_NS = "http://www.openarchives.org/ore/terms/";
var DC_NS = 'http://purl.org/dc/elements/1.1/';
var DCTERMS_NS = 'http://purl.org/dc/terms/';
var FOAF_NS = 'http://xmlns.com/foaf/0.1/';
var OREX_NS = 'http://foresite.cheshire3.org/orex/terms/';
var RDFS_NS = 'http://www.w3.org/2001/01/rdf-schema#';

var conneg_hdrs = {'Accept' : 'application/rdf+xml;q=1.0,application/atom+xml;q=0.9'}

var remoteHost = "http://foresite.cheshire3.org";
var createBackLinks = 0;


// URI -> { prop -> value }
var literalArray = {};
// URI -> { pred -> object }
var linksArray = {};
// URI -> circle element
var nodeArray = {};
// URI -> line element
var edgeArray = {};
// URI -> 1
var doneReMArray = {};
// URI -> content type
var contentTypeArray = {};

var dx = 0;
var dy = 0;
var dragging = 0;
var objectMoved = 0;

var typeLabels = {};

var colorArray = {};
colorArray['ore:aggregates'] = "#FFFFFF";
colorArray['ore:isAggregatedBy'] = "#8D50D5"; 
colorArray['dcterms:references'] = "#C0FFFF";
colorArray['dcterms:isReferencedBy'] = "#FFC0FF";
colorArray['ore:similarTo'] = "#3030FF";
colorArray['rdf:type'] = "#DF6030";
colorArray['rdfs1:isDefinedBy'] = "#DF6060"
colorArray['ore:isDescribedBy'] = "#2A8AC4";
colorArray['dcterms:creator'] = "#DF3040";
colorArray['ore:describes'] = '#000000';
colorArray['orex:hasAnnotation'] = '#10631B';
colorArray['foaf:thumbnail'] = '#633410';
var nodeColorArray = {};
nodeColorArray['external'] = '';
nodeColorArray['expand'] = '';
nodeColorArray['noExpand'] = '';

var dashArray = {};
dashArray['ore:aggregates'] = "";
dashArray['ore:isAggregatedBy'] = ""; 
dashArray['dcterms:references'] = "";
dashArray['ore:similarTo'] = '';
dashArray['rdf:type'] = "8,3";
dashArray['rdfs1:isDefinedBy'] = "8,3";
dashArray['ore:isDescribedBy'] = "8,3";

var relNameArray = {};
relNameArray['dc:title'] = "Title";
relNameArray['dcterms:title'] = "Title";
relNameArray['dc:identifier'] = "Identifier";
relNameArray['dcterms:created'] = "Created";
relNameArray['dcterms:modified'] = "Updated";
relNameArray['dcterms:issued'] = "Published";
relNameArray['dcterms:abstract'] = "Abstract";
relNameArray['dc:rights'] = "Rights";
relNameArray['dc:format'] = "Format";
relNameArray['orex:firstPage'] = "First&#160;Page";
relNameArray['orex:lastPage'] = "Last&#160;Page";
relNameArray['mesur:hasIssue'] = "Issue";
relNameArray['mesur:hasVolume'] = "Volume";
relNameArray['rdfs1:label'] = "Label";
relNameArray['rdf:type'] = "Type";
relNameArray['foaf:thumbnail'] = "Thumbnail";
relNameArray['foaf:depiction'] = "Thumbnail";
relNameArray['foaf:name'] = "Name";
relNameArray['orex:hasAnnotation'] = "Annotation";
relNameArray['dcterms:rights'] = "Rights";
relNameArray['ns:sequence'] = "Sequence";


var linkNameArray = {};
linkNameArray['ore:aggregates'] = "Includes";
linkNameArray['ore:isAggregatedBy'] = "Included In"; 
linkNameArray['dcterms:references'] = "Cites";
linkNameArray['dcterms:isReferencedBy'] = "Cited By";
linkNameArray['ore:similarTo'] = "Web Page";
linkNameArray['rdf:type'] = "Type";
linkNameArray['rdfs1:isDefinedBy'] = "Defined In"
linkNameArray['ore:isDescribedBy'] = "Resource Maps";
linkNameArray['dcterms:creator'] = "Creator";
linkNameArray['ore:describes'] = 'Describes';

linkNameArray['orex:hasAnnotation'] = 'Comment';
linkNameArray['foaf:thumbnail'] = 'Thumbnail';
linkNameArray['dcterms:rights'] = 'Rights';

var defaultLinksArray = {};
defaultLinksArray['ore:aggregates'] = 1;
defaultLinksArray['ore:isAggregatedBy'] = 1;
defaultLinksArray['dcterms:references'] = 1;
defaultLinksArray['dcterms:isReferencedBy'] = 1;
defaultLinksArray['ore:similarTo'] = 1;
defaultLinksArray['dcterms:creator'] = 1;
defaultLinksArray['orex:hasAnnotation'] = 1;

var prefixArray = {};
prefixArray[ORE_NS] = 'ore';
prefixArray[RDF_NS] = 'rdf';
prefixArray[RDFS_NS] = 'rdfs1';  // foresite hack XXX FIX
prefixArray[DC_NS] = 'dc';
prefixArray[DCTERMS_NS] = 'dcterms';
prefixArray[OREX_NS] = 'orex';
prefixArray[FOAF_NS] = 'foaf';


// CSS style for our objects
var styleText = ".circle {\
   stroke: white;\
   stroke-linecap: butt;\
   stroke-linejoin: miter;\
   stroke-width: 3;\
 }\
.nohovercircle {\
   stroke: white;\
   stroke-linecap: butt;\
   stroke-linejoin: miter;\
   stroke-width: 3;\
 }\
.origline {\
   stroke: white;\
   stroke-width: 4;\
   stroke-linecap: butt;\
   stroke-linejoin: miter;\
}\
.line {\
   stroke-width: 4;\
   stroke-linecap: butt;\
   stroke-linejoin: miter;\
 }\
.circle:hover { fill: \#d0d0d0; }\
.background {\
  z-index: -2;\
 background: \#505050;\
 opacity: 0.5;\
 position: absolute;\
 top: 20px;\
 left: 20px;\
 height: 640px;\
 width: 700px;\
 visibility: hidden;\
 }\
.spinnerdiv {\
 z-index: -10;\
 position: absolute;\
 top: 20px;\
 left: 660px;\
 visibility: hidden;\
}\
.canvasdiv {\
 z-index: -1;\
 position: absolute;\
 top: 20px;\
 left: 20px;\
 height: 640px;\
 width: 700px; \
 visibility: hidden;\
 font-size: 9pt;\
 }\
.dataNode {\
  position:absolute;\
  z-index: -1;\
  left: 0px;\
  top: 0px;\
  width:400px; \
  visibility: hidden; \
  background-color: #eeeeee;\
  opacity:0.9; \
  border: 1px solid black;\
  padding: 5px;\
  font-size: 9pt;\
}\
.colorKeyNode {\
position:absolute; \
z-index: -1; \
left: 700px ; \
top: 0px; \
visibility: hidden;\
border: 1px solid black;\
padding: 5px;\
background-color: #B0B0B0;\
opacity: 1.0;\
font-size: 9pt;\
text-align: left;\
}\
.foresite_popout {\
position:absolute;\
z-index: 10000;\
border: 1px solid black;\
top: 20px;\
left: 880px;\
width: 100px;\
padding: 5px;\
background-color: #ffffff;\
opacity: 0.7;\
font-size: 9pt;\
text-align: left;\
}\
#plugin { BACKGROUND: #0d0d0d; COLOR: #AAA; CURSOR: move; DISPLAY: block; FONT-FAMILY: arial; FONT-SIZE: 11px; PADDING: 7px 10px 11px 10px; PADDING-RIGHT: 0; Z-INDEX: -2; POSITION: absolute; WIDTH: 199px; visibility:hidden;}\
#plugin br { CLEAR: both; MARGIN: 0; PADDING: 0;  }\
#plugin select { BORDER: 1px solid #333; BACKGROUND: #FFF; POSITION: relative; TOP: 4px; }\
#plugHEX { FLOAT: left; position: relative; top: -1px; }\
#plugCLOSE { FLOAT: right; cursor: pointer; MARGIN: 0 8px 3px; COLOR: #FFF; -moz-user-select: none; }\
#plugHEX:hover,#plugCLOSE:hover { COLOR: #FFD000;  }\
#plugCUR { float: left; width: 10px; height: 10px; font-size: 1px; background: #FFF; margin-right: 3px; }\
#SV { background: #FF0000 url('http://foresite.cheshire3.org/files/SatVal.png'); POSITION: relative; CURSOR: crosshair; FLOAT: left; HEIGHT: 166px; WIDTH: 167px; MARGIN-RIGHT: 10px; -moz-user-select: none; }\
#SVslide { BACKGROUND: url('http://foresite.cheshire3.org/files/slide.gif'); HEIGHT: 9px; WIDTH: 9px; POSITION: absolute; line-height: 1px; }\
#H { BORDER: 1px solid #000; CURSOR: crosshair; FLOAT: left; HEIGHT: 154px; POSITION: relative; WIDTH: 19px; PADDING: 0; TOP: 4px; -moz-user-select: none; }\
#Hslide { BACKGROUND: url('http://foresite.cheshire3.org/media/slideHue.gif'); HEIGHT: 5px; WIDTH: 33px; POSITION: absolute; line-height: 1px; }\
#Hmodel { POSITION: relative; TOP: -5px; }\
#Hmodel div { HEIGHT: 1px; WIDTH: 19px; font-size: 1px; line-height: 1px; MARGIN: 0; PADDING: 0; }\
";


/* DHTML Color Picker : v1.0.4 : 2008/04/17 */
/* http://www.colorjack.com/software/dhtml+color+picker.html */

function w_plugin(evt) {
  HSVslide('drag', 'plugin', evt);
}

function w_plugHEX(evt) {
  stop=0;
  setTimeout('stop=1', 100);
}

function w_plugCLOSE(evt) {
  toggle('plugin');
}

function w_SV(evt) {
  HSVslide('SVslide', 'plugin', evt);
}

function w_H(evt) {
  HSVslide('Hslide', 'plugin', evt);
}

var maxValue={'H':360,'S':100,'V':100};
var HSV={H:360, S:100, V:100};
var slideHSV={H:360, S:100, V:100};
var zINDEX=10001;
var stop=1;

function S(v,o) { return((typeof(o)=='object'?o:document).getElementById(v)); }
function SS(o) { o=S(o); if(o) return(o.style); }
function abPos(o) { var o=(typeof(o)=='object'?o:S(o)), z={X:0,Y:0}; while(o!=null) { z.X+=o.offsetLeft; z.Y+=o.offsetTop; o=o.offsetParent; }; return(z); }
function toggle(v) { 
  SS(v).visibility = (SS(v).visibility=='hidden'?'visible':'hidden');
  SS(v).zIndex = (SS(v).zIndex<0?10000:-2);
  //SS(v).display = (SS(v).display=='block'?'none':'block');
}
function within(v,a,z) { return((v>=a && v<=z)?true:false); }
function XY(e,v) { var z=[e.pageX,e.pageY]; return(z[zero(v)]); }
function zero(v) { v=parseInt(v); return(!isNaN(v)?v:0); }

function HSVslide(d,o,e) {
  function tXY(e) { tY=XY(e,1)-ab.Y; tX=XY(e)-ab.X; }
  function mkHSV(a,b,c) { return(Math.min(a,Math.max(0,Math.ceil((parseInt(c)/b)*a)))); }
  function ckHSV(a,b) { if(within(a,0,b)) return(a); else if(a>b) return(b); else if(a<0) return('-'+oo); }
  function drag(e) { 
    stop = 0;
    if (!stop) { 
      if(d!='drag') tXY(e);
      if(d=='SVslide') { 
	ds.left=ckHSV(tX-oo,162)+'px'; 
	ds.top=ckHSV(tY-oo,162)+'px';
	slideHSV.S=mkHSV(100,162,ds.left); 
	slideHSV.V=100-mkHSV(100,162,ds.top); 
	HSVupdate();
      }
      else if(d=='Hslide') { 
	var ck=ckHSV(tY-oo,163), r='HSV', z={};
	ds.top=(ck-5)+'px'; 
	slideHSV.H=mkHSV(360,163,ck);
	for(var i in r) { 
	  i=r.substr(i,1); 
	  z[i]=(i=='H')?maxValue[i]-mkHSV(maxValue[i],163,ck):HSV[i]; 
	}
	HSVupdate(z); 
	SS('SV').backgroundColor='#'+color.HSV_HEX({H:HSV.H, S:100, V:100});
      }
      else if(d=='drag') { 
	ds.left=XY(e)+oX-eX+'px'; 
	ds.top=XY(e,1)+oY-eY+'px'; 
      }
    }
  }
  function unmouse() {
    stop = 1;
    S('plugin').removeEventListener('mousemove', drag, true);
    S('plugin').removeEventListener('mouseup', unmouse, true);    
  }
  if(stop) { 
    stop=''; 
    var ds=SS(d!='drag'?d:o);
    if(d=='drag') { var oX=parseInt(ds.left), oY=parseInt(ds.top), eX=XY(e), eY=XY(e,1); SS(o).zIndex=zINDEX++; }
    else { 
      var ab=abPos(S(o)), tX, tY, oo=(d=='Hslide')?2:4; ab.X+=10; 
      ab.Y+=22; 
      if(d=='SVslide') slideHSV.H=HSV.H; 
    }
    S('plugin').addEventListener('mousemove', drag, true);
    S('plugin').addEventListener('mouseup', unmouse, true);
    drag(e);
  }
};

function HSVupdate(v) { 
  v=color.HSV_HEX(HSV=v?v:slideHSV);
  S('plugHEX').innerHTML=v;
  SS('plugCUR').background='#'+v;
  rel = S('plugin').title;
  colorArray[rel] = '#'+v;
  SS(rel).color='#'+v;
  if (edgeArray[rel]) {
    for (e in edgeArray[rel]) {
      edge = edgeArray[rel][e];
      edge.setAttribute('stroke', '#'+v);
    }
  }
  return(v);
};

color={};
color.cords=function(W) {
	var W2=W/2, rad=(hsv.H/360)*(Math.PI*2), hyp=(hsv.S+(100-hsv.V))/100*(W2/2);
	SS('mCur').left=Math.round(Math.abs(Math.round(Math.sin(rad)*hyp)+W2+3))+'px';
	SS('mCur').top=Math.round(Math.abs(Math.round(Math.cos(rad)*hyp)-W2-21))+'px';
};
color.HEX=function(o) { o=Math.round(Math.min(Math.max(0,o),255));
    return("0123456789ABCDEF".charAt((o-o%16)/16)+"0123456789ABCDEF".charAt(o%16));
};
color.RGB_HEX=function(o) { var fu=color.HEX; return(fu(o.R)+fu(o.G)+fu(o.B)); };
color.HSV_RGB=function(o) {
    var R, G, A, B, C, S=o.S/100, V=o.V/100, H=o.H/360;
    if(S>0) { if(H>=1) H=0;
        H=6*H; F=H-Math.floor(H);
        A=Math.round(255*V*(1-S));
        B=Math.round(255*V*(1-(S*F)));
        C=Math.round(255*V*(1-(S*(1-F))));
        V=Math.round(255*V); 
        switch(Math.floor(H)) {
            case 0: R=V; G=C; B=A; break;
            case 1: R=B; G=V; B=A; break;
            case 2: R=A; G=V; B=C; break;
            case 3: R=A; G=B; B=V; break;
            case 4: R=C; G=A; B=V; break;
            case 5: R=V; G=A; B=B; break;
        }
        return({'R':R?R:0, 'G':G?G:0, 'B':B?B:0, 'A':1});
    }
    else return({'R':(V=Math.round(V*255)), 'G':V, 'B':V, 'A':1});
};
color.HSV_HEX=function(o) { return(color.RGB_HEX(color.HSV_RGB(o))); };


function showPicker(evt) {
  span = evt.target;
  if (span.wrappedJSObject) {
    span = span.wrappedJSObject;
  }
  rel = span.getAttribute('id');

  picker = document.getElementById('plugin');
  picker.setAttribute('title', rel);

  // can we go from current hex to HSV?
  HSVupdate({H:0, S:0, V:20});
  picker.style.zIndex = 10000;
  picker.style.visibility = 'visible';
  picker.style.top = evt.pageY + 10 + 'px';
  picker.style.left = evt.pageX - 100 + 'px';

}

function showColorKey() {
  // doing it all by hand to add correct mouse handlers

  d = document.getElementById('colorKeyNode');
  // throw away current nodes
  while(d.childNodes.length > 0) {
    d.removeChild(d.firstChild);
  }

  // title
  s = document.createElement('div');
  s.setAttribute('style', 'font-weight: bold; text-align: center;');
  s.appendChild(document.createTextNode('Foresite Explorer'));
  d.appendChild(s);
  d.appendChild(document.createElement('br'));

  s = document.createElement('div');
  s.setAttribute('style', 'text-decoration: underline; text-align: center;');
  s.appendChild(document.createTextNode('Link Colors'));
  d.appendChild(s);

  s = document.createElement('div');
  s.setAttribute('style', 'text-style: italic; text-align: center;');
  s.appendChild(document.createTextNode('(click to change)'));
  d.appendChild(s);

  d.appendChild(document.createElement('br'));
  
  for (rel in colorArray) {
    c = colorArray[rel];
    if (linkNameArray[rel]) {
      name = linkNameArray[rel];
    } else {
      name = rel;
    }    

    s = document.createElement('div');
    s.setAttribute('style', 'padding-bottom: 1px; padding-top: 0px;');

    cb = document.createElement('input');
    cb.setAttribute('type', 'checkbox');
    cb.setAttribute('id', 'cb_' + rel);
    cb.setAttribute('name', rel);
    if (defaultLinksArray[rel]) {
      cb.setAttribute('checked', 'true');
    }
    s.appendChild(cb);

    s2 = document.createElement('span');
    s2.setAttribute('style', 'padding: 0px; font-weight: bold; color:' + c + ';');
    s2.setAttribute('id', rel);
    s2.addEventListener('click', showPicker, true);
    s2.appendChild(document.createTextNode(' ' + name));
    s.appendChild(s2);
    d.appendChild(s);
  }


  // node colors

  d.appendChild(document.createElement('br'));
  s = document.createElement('div');
  s.setAttribute('style', 'text-decoration: underline; text-align: center;');
  s.appendChild(document.createTextNode('Node Colors'));
  d.appendChild(s);

  s = document.createElement('div');
  s.setAttribute('style', 'font-weight: bold; color: #F0F0F0');
  s.appendChild(document.createTextNode(' * Expands'));
  d.appendChild(s);
  s = document.createElement('div');
  s.setAttribute('style', 'font-weight: bold; color: #707070');
  s.appendChild(document.createTextNode(' * Doesn\'t Expand'));
  d.appendChild(s);
  s = document.createElement('div');
  s.setAttribute('style', 'font-weight: bold; color: #101010');
  s.appendChild(document.createTextNode(' * External Page'));
  d.appendChild(s);
  s = document.createElement('div');
  s.setAttribute('style', 'font-weight: bold; color: #FFB890');
  s.appendChild(document.createTextNode(' * Relationship'));
  d.appendChild(s);

  // credits
  d.appendChild(document.createElement('br'));

  s = document.createElement('div');
  s.setAttribute('style', 'font-weight: bold;');
  s.appendChild(document.createTextNode('By: Rob Sanderson'));
  d.appendChild(s);  
  s = document.createElement('div');
  s.setAttribute('style', 'font-style: italic ;');
  a = document.createElement('a');
  a.setAttribute('href', 'mailto:azaroth@liverpool.ac.uk');
  a.appendChild(document.createTextNode('azaroth@liverpool.ac.uk'));
  s.appendChild(a);
  d.appendChild(s);  

  d.style.visibility = 'visible';
  d.style.zIndex = 7002;
}


function createCanvas() {
    //<div class="canvasdiv" id="svgdiv">

    cvs = document.createElement('div');
    cvs.setAttribute('class', 'canvasdiv');
    cvs.setAttribute('id', 'svgdiv');

    // close [X]
    span = document.createElement('span');
    span.setAttribute('style', 'position: absolute; top: 0px; left: 0px; width: 100%; text-align: right;');
    span.addEventListener('click', function(evt) {hideSvg();}, true);
    b = document.createElement('b');
    b.appendChild(document.createTextNode('[X]'));
    span.appendChild(b);
    cvs.appendChild(span);
    
    //<svg:svg id = "canvas" width="700px" height="640px" viewBox = "0 0 700 640">

    svg = document.createElementNS(SVG_NS, 'svg');
    svg.setAttribute('width', '700px');
    svg.setAttribute('height', '640px');
    svg.setAttribute('viewBox', '0 0 700 640');
    svg.setAttribute('id', 'canvas');
    cvs.appendChild(svg);

    // <circle cx="60" cy="580" r="40" fill="#ffa040"/>
    // main circle
    c = document.createElementNS(SVG_NS, 'circle');
    c.setAttribute('cx', '60');
    c.setAttribute('cy', '580');
    c.setAttribute('r', '40');
    c.setAttribute('fill', '#ffa040');
    svg.appendChild(c);

    //<line x1="43" y1="597" x2="77" y2="563" class="origline"/>
    l = document.createElementNS(SVG_NS, 'line');
    l.setAttribute('x1', '43');
    l.setAttribute('y1', '597');
    l.setAttribute('x2', '77');
    l.setAttribute('y2', '563');
    l.setAttribute('class', 'origline');
    svg.appendChild(l);

    //<line x1="43" y1="597" x2="77" y2="589" class="origline"/>
    l = document.createElementNS(SVG_NS, 'line');
    l.setAttribute('x1', '43');
    l.setAttribute('y1', '597');
    l.setAttribute('x2', '77');
    l.setAttribute('y2', '589');
    l.setAttribute('class', 'origline');
    svg.appendChild(l);

    //<line x1="43" y1="597" x2="51" y2="563" class="origline"/>
    l = document.createElementNS(SVG_NS, 'line');
    l.setAttribute('x1', '43');
    l.setAttribute('y1', '597');
    l.setAttribute('x2', '51');
    l.setAttribute('y2', '563');
    l.setAttribute('class', 'origline');
    svg.appendChild(l);

    //<circle cx="77" cy="563" r="9.5" fill="#909090" class="circle" id="start" onclick="explode(this)"/>
    c = document.createElementNS(SVG_NS, 'circle');
    c.setAttribute('cx', '77');
    c.setAttribute('cy', '563');
    c.setAttribute('r', '9.5');
    c.setAttribute('fill', '#909090');
    c.setAttribute('class', 'circle');
    c.setAttribute('id', 'start');
    c.setAttribute('onlick', 'explode(this)');
    svg.appendChild(c);

    //<circle cx="77" cy="589" r="9.5" fill="#909090" class="circle"/>
    c = document.createElementNS(SVG_NS, 'circle');
    c.setAttribute('cx', '77');
    c.setAttribute('cy', '589');
    c.setAttribute('r', '9.5');
    c.setAttribute('fill', '#909090');
    c.setAttribute('class', 'nohovercircle');
    svg.appendChild(c);

    //<circle cx="51" cy="563" r="9.5" fill="#909090" class="circle"/>
    c = document.createElementNS(SVG_NS, 'circle');
    c.setAttribute('cx', '51');
    c.setAttribute('cy', '563');
    c.setAttribute('r', '9.5');
    c.setAttribute('fill', '#909090');
    c.setAttribute('class', 'nohovercircle');
    svg.appendChild(c);

    //<circle cx="43" cy="597" r="10" fill="black" class="circle"/>
    c = document.createElementNS(SVG_NS, 'circle');
    c.setAttribute('cx', '43');
    c.setAttribute('cy', '597');
    c.setAttribute('r', '9.5');
    c.setAttribute('fill', 'black');
    c.setAttribute('class', 'nohovercircle');
    svg.appendChild(c);

    // <div id="dataNode" class="dataNode"/>

    d = document.createElement('div');
    d.setAttribute('id', 'dataNode');
    d.setAttribute('class', 'dataNode');
    cvs.appendChild(d);

    
    //<div id="colorKeyNode" 

    ck = document.createElement('div');
    ck.setAttribute('id', 'colorKeyNode');
    ck.setAttribute('class', 'colorKeyNode');
    cvs.appendChild(ck);


    // Color Picker
    cp = document.createElement('div');
    cp.setAttribute('id', 'plugin');
    cp.setAttribute('style', 'top: 58px; z-index: -2;');
    
    cp2 = document.createElement('div');
    cp2.setAttribute('id', 'plugCUR');
    cp.appendChild(cp2);

    cp2 = document.createElement('div');
    cp2.setAttribute('id', 'plugHEX');
    cp2.addEventListener('mousedown', w_plugHEX, true);
    cp2.appendChild(document.createTextNode('FFFFFF'));
    cp.appendChild(cp2);
    
    cp2 = document.createElement('div');
    cp2.setAttribute('id', 'plugCLOSE');
    cp2.addEventListener('mousedown', w_plugCLOSE, true);
    cp2.appendChild(document.createTextNode("[X]"));
    cp.appendChild(cp2);

    cp2 = document.createElement('br');
    cp.appendChild(cp2);

    cp2 = document.createElement('div');
    cp2.setAttribute('id', 'SV');
    cp2.setAttribute('title', 'Saturation + Value');
    cp2.addEventListener('mousedown', w_SV, true);
    
    cp3 = document.createElement('div');
    cp3.setAttribute('id', 'SVslide');
    cp3.setAttribute('style', 'top: -4px; left: -4px;');
    cp2.appendChild(cp3);
    cp4 = document.createElement('br');
    cp3.appendChild(cp4);
    cp.appendChild(cp2);

    cp2 = document.createElement('form');
    cp2.setAttribute('id', 'H');
    cp2.setAttribute('title', 'Hue');
    cp2.addEventListener('mousedown', w_H, true);
    
    cp3 = document.createElement('div');
    cp3.setAttribute('id', 'Hslide');
    cp3.setAttribute('style', 'top: -7px; left: -8px;');
    cp4 = document.createElement('br');
    cp3.appendChild(cp4);
    cp2.appendChild(cp3);
    
    cp3 = document.createElement('div');
    cp3.setAttribute('id', 'Hmodel');

    z = "";
    for(var i=165; i>=0; i--) { z+="<div style=\"BACKGROUND: #"+color.HSV_HEX({H:Math.round((360/165)*i), S:100, V:100})+";\"><br /><\/div>"; }
    cp3.innerHTML=z;

    cp2.appendChild(cp3);
    cp.appendChild(cp2);

    cvs.appendChild(cp);

    return cvs;
}



// ============== AJAX ==============

getResponseXML = function(resp) {
  if (resp._responseXML) {
    return resp._responseXML;
  }
  return resp._responseXML = new DOMParser().parseFromString(resp.responseText, 'text/xml');
}
  
  
function realReceiveFn(url, what, resp) {

  xml = getResponseXML(resp);

  elms = xml.getElementsByTagName('div');
  if (elms.length == 1) {
    // we don't have the data :(
    hideSpinner();      
    doneReMArray[url] = 1;
    alert("Object not in Foresite Dataset :(");
    return;

  } else {
    // RDF-XML
    elms = xml.childNodes[0].childNodes;
    if (elms.length == 0) {
      elms = xml.childNodes[1].childNodes;
    }
    for (var e=0 ; e < elms.length; e++) {
      processRdfXml(elms[e]);
    }
  } 
  
  hideSpinner();      
  doneReMArray[url] = 1;
  svgnode = document.getElementById('canvas');
  if (isInArray(what, svgnode.childNodes)) {
    realExplode(what, 1);
  }
}


errorFn = function(resp) {
  // probably not XML
  txt = resp.responseText;
  GM_log(txt);
  hideSpinner();      
}


function sendRequest(url, what) {
  showSpinner(what);
  GM_xmlhttpRequest({method : "GET",
			url : url,
			headers : conneg_hdrs,
			onload : function(resp) {
			realReceiveFn(url, what, resp);
		      }
		    });
}

function getHeaders(what) {
  GM_xmlhttpRequest({method : 'HEAD',
			url : what.uri,
			headers : conneg_hdrs,
			onload : function(resp) {
			headReceiveFn(what, resp);
		      }});
}

function headReceiveFn(what, resp) {
  headers = resp.responseHeaders;
  headList = headers.split('\n');
  for (h in headList) {
    hdr = headList[h].toLowerCase();
    if (hdr.indexOf('content-type') > -1) {
      var e = hdr.indexOf(';')
	if (e == -1) {
	  e = hdr.length;
	}
      ct = hdr.substring(hdr.indexOf(':')+1, e);
      ct = ct.replace(' ', '');
      contentTypeArray[what.uri] = ct;
      if (ct == 'application/rdf+xml' || ct == 'application/atom+xml') {
	// reset color
	what.setAttribute('fill', '#C0C0C0');
      }
      if (resp.status == 200) {
	return;
      }
    } else if (hdr.indexOf('location:') == 0) {
      // redirect, reissue request and fail on this one
      loc = hdr.substring(hdr.indexOf('http:'), hdr.length);
      loc = loc.replace(' ', '');
      what.uri = loc;
      getHeaders(what);
      return;
    }
  }
}


function showSpinner(what) {
  x = document.getElementById('spinner');
  x = x.wrappedJSObject;
  x.style.visibility = 'visible';
  x.style.zIndex = "6000";
  x.style.top = what.cy.baseVal.value + 30 + "px";
  x.style.left = what.cx.baseVal.value + 30 + "px";   
}

function hideSpinner() {
  x = document.getElementById('spinner');
  x.style.visibility='hidden';
  x.style.zIndex = -1;
}



// ======== RDF Processing ========


function processRdfXml(d) {
  // <rdf:Description rdf:about="subj">
  //   <ns:Prop>literal-objt</ns:Prop>
  //   <ns2:Rel rdf:resource="objt"/>

  if (d.nodeType != 1) {
    return;
  }
  subj = d.getAttributeNS(RDF_NS, 'about');
  if (!subj) {
    subj = d.getAttributeNS(RDF_NS, 'nodeID');
  }
  subj = subj.replace('%2C', ',');

  if (subj.charAt(0) == '/') {
    subj = "http://chroniclingamerica.loc.gov" + subj;
  }
  
  if (subj) {
    if (!literalArray[subj]) {
      info = {};
      links = {};
    } else {
      info = literalArray[subj];
      links = linksArray[subj];
    }
    var hasType = {};

    for ( var e3 = 0; e3 < d.childNodes.length ; e3++) {
      c3 = d.childNodes[e3];
      if (c3.nodeType != 1) {
	continue;
      }

      var nsuri = c3.namespaceURI;
      var pref = prefixArray[nsuri];
      if (!pref) {
	pref = 'ns';
      }

      pred = pref + ":" + c3.localName;

      objt = c3.getAttributeNS(RDF_NS, 'resource');
      if (!objt) {
	objt = c3.getAttributeNS(RDF_NS, 'nodeID');
      }      

      if (objt) {
	if (objt.charAt(0) == '/') {
	  objt = "http://chroniclingamerica.loc.gov" + objt;
	}
	objt = objt.replace('%2C', ',');

	if (pred == 'ore:describes') {
	  tempAggrURI = objt;
	}

	if (pred == 'rdf:type') {
         typeArray = info[pred];
         if (!typeArray) {
           typeArray = {};
         }
         typeArray[objt] = objt;
         info[pred] = typeArray;
	} else if (pred == 'foaf:thumbnail') {
	  info[pred] = objt;
	} else if (pred == 'foaf:depiction') {
	  pred = 'foaf:thumbnail';
	  info[pred] = objt;
	}

	if (debug) {
	  alert(subj + ' ' + pred + ' ' + objt);
	}
	
	
	mylinks = links[pred];
	if (mylinks == undefined) {
	  mylinks = [];
	}
	if (!isInArray(objt, mylinks)) {
	  mylinks.push(objt);
	}
	links[pred] = mylinks;
	
      } else {
	if (c3.childNodes.length) {
	  data = c3.childNodes[0].nodeValue;
	} else {
	  continue;
	}
	info[pred] = data;
	if (pred == "rdfs1:label") {
	  typeLabels[subj] = data;
	}
      }
    }
    literalArray[subj] = info;
    linksArray[subj] = links;
  }
}


// =========== SVG Mouse Events ===========

function mousemove_listener(evt) {
  objectMoved = 1;
  var id = dragging.ownerSVGElement.suspendRedraw(1000);
  deltax = evt.clientX + dx - dragging.cx.baseVal.value; 
  deltay = evt.clientY + dx - dragging.cy.baseVal.value; 
  dragging.moveBy(deltax, deltay, 0);
  dragging.ownerSVGElement.unsuspendRedraw(id);
  node_mouseout_listener(evt);
}

function mouseup_listener(evt) {

  document.removeEventListener("mousemove", mousemove_listener, true);
  document.removeEventListener("mouseup", mouseup_listener, true);
  svgnode = document.getElementById('canvas');

  var id = svgnode.suspendRedraw(1000);

  if (!objectMoved) {
    if (evt.shiftKey) {
      // Go to the target URI
      u = dragging.uri;
      info = linksArray[u];
      if (info) {
	st = info['ore:similarTo'];
	if (st) {
	  u = st[0];
	}
      }
      svgnode.unsuspendRedraw(id);  
      dragging = 0;
      objectMoved = 0;
      var answer = confirm('Go to page: ' + u + '?');
      if (answer) {
	location.href = u;
      }
      return;
    } else {
      explode(dragging);
    }
  } else if (dragging) {
    // we've finished moving
    try {
      if (dragging.outEdges.length) {
	// re-draw
	reExplode(dragging);
      }
    } catch (err) { 
    }
  }  
  try {
    svgnode.removeChild(dragging);
    svgnode.appendChild(dragging);
  } catch (err) {
  }
  svgnode.unsuspendRedraw(id);  
  dragging = 0;
  objectMoved = 0;
  dx = 0;
  dy = 0;
}

function node_mousedown_listener(evt) {
  
  document.addEventListener('mouseup', mouseup_listener, true);
  document.addEventListener("mousemove", mousemove_listener, true);
  if (dragging == 0) {
    dragging = evt.target;
    if (dragging.wrappedJSObject != undefined) {
      dragging = dragging.wrappedJSObject;
    }
    dx = dragging.cx.baseVal.value - evt.clientX;
    dy = dragging.cy.baseVal.value - evt.clientY;
    svgnode = document.getElementById('canvas');
    var id = dragging.ownerSVGElement.suspendRedraw(1000);
    svgnode.insertBefore(dragging, svgnode.childNodes[0]);  
    dragging.ownerSVGElement.unsuspendRedraw(id);
  }
    
}

function node_mouseover_listener(evt) {
  // show data
  p = evt.target;
  if (p.wrappedJSObject != undefined) {
    p = p.wrappedJSObject;
  }
  
  d = document.getElementById('dataNode')
  d.style.top =  p.cy.baseVal.value + 15 + 'px';
  d.style.left = p.cx.baseVal.value + 15 + 'px';

  if (p.uri) {
    // construct from all known nodes
    
    if (p.uri.indexOf('[_:') > -1) {
      data = "<table width='100%'>";
    } else {
      data = "<table width='100%'><tr><td colspan='2'><center><i>" + p.uri + "</i></center></td></tr>";
    }
    info = literalArray[p.uri];
    keys = [];
    var doneArray = {};
    // dc:title/dcterms:title first if possible
    if (info) {
      if (info['dc:title'] != undefined) {
	data += '<tr><td valign="top" align="right"><i>Title:</i></td><td><b>' + info['dc:title'] + "</b></td></tr>";
	doneArray['dc:title'] = 1;
      }      
      if (info['dcterms:title'] != undefined) {
	data += '<tr><td valign="top" align="right"><i>Title:</i></td><td><b>' + info['dcterms:title'] + "</b></td></tr>";
	doneArray['dcterms:title'] = 1;
      }      
      
      // get if rdf:type is checked or not
      typElm = document.getElementById('cb_rdf:type');
      noShowType = typElm.getAttribute('checked');

      for (var i in info) {
	if (!doneArray[i]) {
	  if (relNameArray[i]) {
	    n = relNameArray[i];
	  } else {
	    n = i.substring(i.indexOf(':')+1, i.length);
	    n = n.charAt(0).toUpperCase() + n.substring(1, n.length);
	  }
	  if (i == "rdf:type" && !noShowType) {
	    val = "";
	    for (var x in info[i]) {
	      if (typeLabels[info[i][x]]) {
		val += typeLabels[info[i][x]];
	      } else {
		val += info[i][x];
	      }
	      val += "<br/> ";
	    }
	    val = val.substring(0, val.length-6);
	  } else if (i == "foaf:thumbnail") {
	    val = '<img src="'+ info[i] +'"></img>';
	  } else if (i == 'foaf:depiction') {
	    val = '<img src="'+ info[i] +'"></img>';
	  } else {
	    val = info[i];
	  }
	  data += '<tr><td valign="top" align="right"><i>' + n + ":</i></td><td>" + val + "</td></tr>";
	}
      }
    }
    data += "</table>";
  } else {
    if (linkNameArray[p.rel]) {
      data = "<i>" + linkNameArray[p.rel] + "</i>";
    } else {
      data = "<i>" + p.rel + "</i>";    
    }
  }
  d.innerHTML = data;
  d.style.visibility = 'visible';
  d.style.zIndex = 20000;

}

function node_mouseout_listener(evt) {
  // hide data
  d = document.getElementById('dataNode');
  d.style.top =  '0px';
  d.style.left = '0px';
  d.style.visibility = 'hidden';
  d.style.zIndex = -2;
}


// =========== SVG Object Creation ===========

function create_edge(from, to) {
  l = document.createElementNS(SVG_NS, 'line');
  if (l.wrappedJSObject != undefined) {
    l = l.wrappedJSObject;
  }

  fromCenter = from.getCenter();
  toCenter = to.getCenter();
  l.setAttribute('x1', fromCenter[0]);
  l.setAttribute('y1', fromCenter[1]);
  l.setAttribute('x2', toCenter[0]);
  l.setAttribute('y2', toCenter[1]);
  l.setAttribute('class', 'line');
  l.fromNode = from;
  l.toNode = to;
  l.backLink = 0;
  to.inEdge = l;
  from.outEdges.push(l);
  l.moveBy = edge_moveBy;
  return l;
}


function create_node(parent, x, y) {
  c2 = document.createElementNS(SVG_NS, 'circle');
  if (c2.wrappedJSObject != undefined) {
    c2 = c2.wrappedJSObject;
  }

  c2.setAttribute('cx', x);
  c2.setAttribute('cy', y);
  c2.setAttribute('r', 9);
  c2.setAttribute('class', 'circle');
  c2.moveBy = node_moveBy;
  c2.getCenter = node_getCenter;
  c2.parent = parent;
  c2.outEdges = new Array();
  c2.inBackLinks = new Array();
  c2.contract = node_contract;
  c2.addEventListener("mousedown", node_mousedown_listener, false)
  c2.data = "";
  c2.addEventListener("mouseover", node_mouseover_listener, false)
  c2.addEventListener("mouseout", node_mouseout_listener, false)
  return c2;
}

edge_moveBy = function(fromNode,toNode) {
  this.x1.baseVal.value = fromNode.cx.baseVal.value;
  this.y1.baseVal.value = fromNode.cy.baseVal.value;
  this.x2.baseVal.value = toNode.cx.baseVal.value;
  this.y2.baseVal.value = toNode.cy.baseVal.value;
}

node_moveBy = function(x,y,inner) {
  this.cx.baseVal.value += x;
  this.cy.baseVal.value += y;
  if (!inner) {
    this.inEdge.x2.baseVal.value += x;
    this.inEdge.y2.baseVal.value += y;
  }
  for(var i = 0; i < this.outEdges.length; i++) { 
    var edge = this.outEdges[i];
    if (!edge.backLink) {
      edge.toNode.moveBy(x,y, 1);
    }
    edge.moveBy(this, edge.toNode);
  }
}   


node_getCenter = function() {
  x = new Array();
  x.push(this.cx.baseVal.value);
  x.push(this.cy.baseVal.value);
  return x;
}

node_contract = function() {
  svgnode = document.getElementById('canvas');
  for (var e=0; e<this.outEdges.length; e++) {
    var edge = this.outEdges[e];
    if (!edge.backLink) {
      edge.toNode.contract();
      svgnode.removeChild(edge);
    } else {
      svgnode.removeChild(edge);
    }
  }
  svgnode.removeChild(this);
  // destroy in back links
  for (var x=0; x < this.inBackLinks.length; x++) {
    try {
      svgnode.removeChild(this.inBackLinks[x]);    
    } catch (err) {}
  }
  this.inBackLinks = new Array();
  
  // How to destroy completely?
  this.removed = 1;
  if (this.uri) {
    nodeArray[this.uri] = undefined;
  }
}


function isRemAggr(what) {
  typ = '';
  fmt = contentTypeArray[what.uri];
  idb = '';
  ds = '';

  inRel = what.inEdge.fromNode.rel;
  lits = literalArray[what.uri];
  links = linksArray[what.uri];
  if (lits) {
    fmt = lits['dc:format'];
    typ = lits['rdf:type'];
  }
  if (links) {
    idb = links['ore:isDescribedBy'];
    ds = links['ore:describes'];
    if (!typ) {
      typ = links['rdf:type'];
    }
  }
	  
  if (fmt == 'application/atom+xml' || fmt == 'application/rdf+xml') {
    // probably a resource map
    return 1;
  } else if (idb || ds) {
    return 1;
  } else if (fmt) {
    return 0;
  } else if (inRel == 'ore:isAggregatedBy') {
    return 1;
  } else if (typ) {
    for (i in typ) {
      if (typ[i] == 'ore:Aggregation' || typ[i] == 'http://www.openarchives.org/ore/terms/Aggregation' || typ[i] == 'Aggregation') {
	return 1;
      }
    }
  }	    
  if (what.uri.indexOf('http') == 0) {
    getHeaders(what);
  }
  return -1;
}

function explode(what) {

  if (what.wrappedJSObject != undefined) {
    what = what.wrappedJSObject;
  }
  
  if (what.outEdges.length > 0) {
    svg = document.getElementById('canvas');
    var id = svg.suspendRedraw(1000);
    for (var e=0; e<what.outEdges.length; e++) {
      var edge = what.outEdges[e];
      if (!edge.backLink) {
	edge.toNode.contract();
	svg.removeChild(edge);
      } else {
	svg.removeChild(edge);
      }	
    }
    what.outEdges = new Array();
    svg.unsuspendRedraw(id);
  } else {

    if (what.rel) {
      // we're a relationship, so explode to links
      realExplode(what, 0);

    } else {
      // we're an object, so explode to relationships
      // or go to page

      if (doneReMArray[what.uri]) {
	// already exist
	realExplode(what, 1);
      } else {
	// don't have any information about node yet

	// should we try to fetch?
	if (what.id == 'start') {
	  // start node!
	  sendRequest(what.uri, what);
	} else {
	  // first check in links for dc:format
	  
	  if (isRemAggr(what)) {
	    // NB -1 (eg we don't know) is true
	    sendRequest(what.uri, what);    	    
	  } else {
	    var answer = confirm('Go to page: ' + what.uri + '?');
	    if (answer) {
	      location.href = what.uri;
	    }
	  }
	}
      }
    }
  }
}

function realExplode(what, isRel) {
  svg = document.getElementById('canvas');
  
  if (svg.wrappedJSObject != undefined) {
    svg = svg.wrappedJSObject;
  }

  if (isRel) {

    if (tempAggrURI) {
      what.uri = tempAggrURI;
      tempAggrURI = '';
    }
    if (what.uri.indexOf('http://foresite.cheshire3.org/txr/') == 0) {
      what.uri = what.uri.substring(34, what.uri.length);
    }

    links = linksArray[what.uri];

    bits = [];
    for (var l in links) {
      if (l.wrappedJSObject) {
	l = l.wrappedJSObject;
      }
      cb = document.getElementById('cb_' + l);
      if (!cb) {
	// unexpected, show
	bits.push(l);
      } else {
	if (cb.wrappedJSObject) {
	  cb = cb.wrappedJSObject;
	}
	// getAttribute('checked') doesn't work
	if (cb.checked) {
	  bits.push(l);
	}
      }
    }
    bits.sort();
    bits.reverse();

  } else {
    links = linksArray[what.parent.uri];
    bits = links[what.rel];
    // check for jstor pages and resort by number

    // Necessary URI dependence
    if (bits[0].indexOf('www.jstor.org') > -1 && bits[0].indexOf('seq') > -1) {
      bits.sort(function(a,b) {
		  if (a.indexOf('pdfplus') > -1) {
		    return 1;
		  } else if (b.indexOf('pdfplus') > -1) {
		    return -1;
		  } else {
		    return a.substring(a.indexOf('seq')+4, a.length) - b.substring(b.indexOf('seq')+4, b.length);
		  }
		});
    } else if (bits[0].indexOf('chroniclingamerica.loc.gov') > -1 && bits[0].indexOf('seq-') > -1) {
      bits.sort(function(a,b) {		  
		  return a.substring(a.indexOf('seq-')+4, a.indexOf('#')) - b.substring(b.indexOf('seq-')+4, b.indexOf('#'));
		});
    } else {
      bits.sort();
    }
    bits.reverse();
  }

  n = bits.length;
  maxAngle = 20;    
  
  angle = 225/n;
  if (angle > maxAngle) {
    angle = maxAngle;
  }
    
  mid = angle * ((n-1)/2);

  if (what.inEdge == undefined) {
    inAngle = 45;
    dtheta = 90;
  } else {
    inNode = what.inEdge.fromNode;
    
    a = what.cx.baseVal.value - inNode.cx.baseVal.value;
    o = inNode.cy.baseVal.value - what.cy.baseVal.value;
    ooa = o/a;
    inAngle = Math.atan(ooa);      
    inAngle = inAngle * (180/Math.PI);

    var neg = inAngle > 0;
    var up = what.cy.baseVal.value > inNode.cy.baseVal.value;
    if (neg && up) {
      dtheta = -90;
    } else if (neg) {
      dtheta = 90;
    } else if (up) {
      dtheta = 90;
    } else {
      dtheta = -90;
    }
  }
    
  dtheta += inAngle - mid;
  c = what.getCenter();
  nx = c[0];
  ny = c[1];
  if (n < 8) {
    baseDist = 60;
  } else {
    baseDist = 7 * n;
    if (baseDist > 180) {
      baseDist = 260;
    }
  }

  for (var x = 0; x<n; x++) {
    var ma = angle * x;
    ma2 = ma + dtheta;
    radians = ma2 * Math.PI/180;
    sin = Math.sin(radians);
    cos = Math.cos(radians);

    if (createBackLinks && !isRel && nodeArray[bits[x]] != undefined) {
      c2 = nodeArray[bits[x]];
      backLink = 1;
    } else {
      c2 = create_node(what, (nx + (baseDist * sin)), ny+(baseDist*cos));
      backLink = 0;
    }

    l2 = create_edge(what, c2);
    l2.backLink = backLink;
    if (backLink) {
      what.inBackLinks.push(l2);
    }

    if (isRel) {
      c2.rel = bits[x];
      c2.uri = "";
      c2.setAttribute('fill', '#ffb890');
      if (colorArray[c2.rel]) {
	l2.setAttribute('stroke', colorArray[c2.rel]);
      } else {
	l2.setAttribute('stroke', "#ffb890");
      }
      if (dashArray[c2.rel]) {
	l2.setAttribute('stroke-dasharray', dashArray[c2.rel]);
      }

      ea = edgeArray[c2.rel];
      if (!ea) {
	ea = new Array();
      }
      ea.push(l2);
      edgeArray[c2.rel] = ea;
      
    } else {
      c2.rel = "";
      c2.uri = bits[x];

      if (isRemAggr(c2) == 1) {
	c2.setAttribute('fill', '#C0C0C0');
      } else if (linksArray[c2.uri]) {
	external = 1;
	for (i in linksArray[c2.uri]) {
	  if (i != 'rdf:type' && i != 'dcterms:creator') {
	    external = 0;
	    break;
	  }
	}
	if (external) {
	  c2.setAttribute('fill', '#101010');
	} else {
	  c2.setAttribute('fill', '#707070');
	}
      } else {
	c2.setAttribute('fill', '#101010');
      }

      if (colorArray[what.rel]) {
	l2.setAttribute('stroke', colorArray[what.rel]);
      } else {
	l2.setAttribute('stroke', '#ffb890');
      }
      if (dashArray[what.rel]) {
	l2.setAttribute('stroke-dasharray', dashArray[what.rel]);
      }
      nodeArray[c2.uri] = c2;
      ea = edgeArray[what.rel];
      if (!ea) {
	ea = new Array();
      }
      ea.push(l2);
      edgeArray[what.rel] = ea;
    }

    svg.appendChild(l2);
    svg.appendChild(c2);
  }
  try {
    svg.removeChild(what);
    svg.appendChild(what);
  } catch (err) {
  }
}


function reExplode(what) {
  // Move stuff around but don't recreate

  svg = document.getElementById('canvas');
  var n = what.outEdges.length;
  angle = 225/n;
  if (angle > maxAngle) {
    angle = maxAngle;
  }
    
  mid = angle * ((n-1)/2);
  if (what.inEdge == undefined) {
    inAngle = 45;
    dtheta = 90;
  } else {
    inNode = what.inEdge.fromNode;
    
    a = what.cx.baseVal.value - inNode.cx.baseVal.value;
    o = inNode.cy.baseVal.value - what.cy.baseVal.value;
    ooa = o/a;
    inAngle = Math.atan(ooa);      
    inAngle = inAngle * (180/Math.PI);

    var neg = inAngle > 0;
    var up = what.cy.baseVal.value > inNode.cy.baseVal.value;
    if (neg && up) {
      dtheta = -90;
    } else if (neg) {
      dtheta = 90;
    } else if (up) {
      dtheta = 90;
    } else {
      dtheta = -90;
    }
  }
    
  dtheta += inAngle - mid;
  c = what.getCenter();
  nx = c[0];
  ny = c[1];
  if (n < 8) {
    baseDist = 60;
  } else {
    baseDist = 7 * n;
    if (baseDist > 180) {
      baseDist = 260;
    }
  }

  for (var x = 0; x<n; x++) {
    var ma = angle * x;
    ma2 = ma + dtheta;
    radians = ma2 * Math.PI/180;
    sin = Math.sin(radians);
    cos = Math.cos(radians);

    var edge = what.outEdges[x];
    var toNode = edge.toNode;
    
    // move existing objects to new locations
    if (!edge.backLink) {
      toNode.cx.baseVal.value = nx + (baseDist * sin);
      toNode.cy.baseVal.value = ny + (baseDist * cos);
    }
    edge.moveBy(what, toNode);
  }

  for (var x = 0 ; x<n ; x++) {
    var edge = what.outEdges[x];
    if (!edge.backLink && edge.toNode.outEdges.length > 0) {
      reExplode(edge.toNode);
    }
  }
  try {
    svg.removeChild(what);
    svg.appendChild(what);
  } catch (err) {
  }

}


function init() {
  s = document.getElementById('start');
  s = s.wrappedJSObject;
  s.getCenter = node_getCenter;
  s.outEdges = new Array();
  s.contract = node_contract;
  s.addEventListener("mousedown", node_mousedown_listener, false);
  s.addEventListener("mouseover", node_mouseover_listener, false);
  s.addEventListener("mouseout", node_mouseout_listener, false);
  s.uri = aggrURI;
  s.rel = '';
  function f() {};
  s.moveBy = f;
  nodeArray[aggrURI] = s;
  s.inBackLinks = new Array();
}


function showSvg(evt) {
  d = document.getElementById('svgbackground');
  d.style.visibility = 'visible';
  d.style.zIndex = 4000;
  d = document.getElementById('svgdiv');
  d.style.visibility = 'visible';
  d.style.zIndex = 4001;

  showColorKey()

  s = document.getElementById('start');
  if (s.wrappedJSObject) {
    s = s.wrappedJSObject;
  }
  // contract any open links (eg reclicked)
  for (var e=0; e<s.outEdges.length; e++) {
    var edge = s.outEdges[e];
    edge.toNode.contract();
    svgnode.removeChild(edge);    
  }
  s.outEdges = new Array();

  d = document.getElementById('dataNode')
  d.style.top =  s.cy.baseVal.value + 15 + 'px';
  d.style.left = s.cx.baseVal.value + 15 + 'px';

  data = "<b>Start here by clicking the middle grey node</b>";
  d.innerHTML = data;
  d.style.zIndex = 4001;
  d.style.visibility = "visible";
  
  scroll(0,0);

}

function hideSvg(evt) {
  d = document.getElementById('svgbackground');
  d.style.visibility = 'hidden';
  d.style.zIndex = -1;
  d = document.getElementById('svgdiv');
  d.style.visibility = 'hidden';
  d.style.zIndex = -1;
  d = document.getElementById('colorKeyNode');
  d.style.visibility = 'hidden';
  d.style.zIndex = -1;
}

function isInArray(obj, array) {
  for(var i = 0; i < array.length; i++) { 
    if(obj == array[i]) { 
      return true;
    }
  } 
  return false;
}




// ------------------------------------
//
// greasemonkey injection starts here
//
// ------------------------------------



var done = 0;

// Pretty injection scripts for JSTOR and ChronAm

if (here.indexOf('www.jstor.org') > -1) {

  var id = here.substring(28, 35);
  var ezproxy = 0;
  if (id[0] == '.') {
    // www.jstor.org.ezproxy.liv.ac.uk
    id = here.substring(28 + 18, 35 + 18);
    ezproxy = 1;
  } 
  if (id[6] == '?') {
    id = id.substring(0, 6);
  }
  var aggrURI = "http://foresite.cheshire3.org/stable/ore/" + id;

  var google = document.getElementById('googleLinks');
  if (google) {
    // We're an article
  
    br = document.createElement('br');
    n = google.childNodes.length;
    google.insertBefore(br, google.childNodes[n-1]);

    newDiv = document.createElement('div');
    newDiv.setAttribute('id', 'oreLinks');
    // have to hand construct to get it to add eventListener properly

    ul = document.createElement('ul');
    ul.setAttribute('class', 'relatedLinks');
    newDiv.appendChild(ul);
    li = document.createElement('li');
    ul.appendChild(li);
    
    a = document.createElement('a');
    a.setAttribute('href', aggrURI);  
    li.appendChild(a);
    img = document.createElement('img');
    img.setAttribute('src', 'http://www.openarchives.org/ore/logos/ore_logo_14.png');
    a.appendChild(img);
    
    li.appendChild(document.createTextNode('  '));
    
    a = document.createElement('a');
    a.setAttribute('href', 'javascript:none;');
    a.appendChild(document.createTextNode('ExplORE'));
    a.addEventListener('click', showSvg, true);
    li.appendChild(a);
    
    google.parentNode.insertBefore(newDiv, google.nextSibling);
    
    titleDiv = document.createElement('div');
    titleDiv.setAttribute('class', 'relatedHeading');
    titleDiv.innerHTML = '<a class="noHover" name="googleLinks" onClick="expandCollapse(\'oreLinks\',\'oreLinksImage\')"><img id="oreLinksImage" alt="Collapse" src="/templates/jsp/_jstor/images/collapse_white.gif"/></a><h2>OAI: ORE</h2>';
    
    google.parentNode.insertBefore(titleDiv, google.nextSibling);
    done = 1
  
  } else {  
    sbc = document.getElementById('searchBoxContainer');

    if (sbc) {
      // We're an issue or a journal
      newDiv = document.createElement('div');
      newDiv.setAttribute('id', 'oreLinks');
      newDiv.setAttribute('class', 'accessNote');
      
      br = document.createElement('br');
      newDiv.appendChild(br);
      a = document.createElement('a');
      a.setAttribute('href', aggrURI);  
      newDiv.appendChild(a);
      img = document.createElement('img');
      img.setAttribute('src', 'http://www.openarchives.org/ore/logos/ore_logo_14.png');
      a.appendChild(img);

      newDiv.appendChild(document.createTextNode('  '));

      a = document.createElement('a');
      a.setAttribute('href', 'javascript:none;');
      a.appendChild(document.createTextNode('ExplORE'));
      a.addEventListener('click', showSvg, true);
      newDiv.appendChild(a);
      
      sbc.parentNode.insertBefore(newDiv, sbc.lastSibling);
      done = 1;
    } 
  }
    
 } else if (here.indexOf('flickr') > -1) {
  // Flickr 
  // Discover correct foresite aggregation URI
  if (here.indexOf('www.flickr.com/photos/') > -1) {
    // photoId is last section
    here = here.replace('/in/photostream/', '');
    
    slidx = here.lastIndexOf('/');
    if (slidx == here.length -1) {
      here = here.substring(0, here.length-1);
      slidx = here.lastIndexOf('/');
    }
    
    pid = here.substr(slidx+1, here.length-slidx);
    if (here.indexOf('/sets/') > -1) {
      id = "set/" + pid;
    } else if (pid.indexOf('@') > -1) {
      id = "user/" + pid;
    } else if (pid.match('^[0-9]+$')) {
      id = "photo/" + pid;
    } else {
      // named user
      id = "user/" + pid;
    }
    
  } else if (here.indexOf('www.flickr.com/search/') > -1) {
    // dig out q=...& and link to search
    qidx = here.indexOf('q=');    
    if (qidx > -1) {
      aidx = here.indexOf('&', qidx+1);
      if (aidx == -1) {
	aidx = here.length;
      }
      query = here.substring(qidx+2, aidx);
      id  = "search/" + query;
    } else {
      return;
    }
    
  } else if (here.indexOf('www.flickr.com/groups/') > -1) {
    slidx = here.lastIndexOf('/');
    if (slidx == here.length -1) {
      here = here.substring(0, here.length-1);
      slidx = here.lastIndexOf('/');
    }
    pid = here.substr(slidx+1, here.length-slidx);
    id = "pool/" + pid;
    
  } else {
    // no idea!
    return;
  }

  id = id.replace('+', '%20');
  aggrURI = "http://foresite.cheshire3.org/flickr/ore/" + id;

 } else if (here.indexOf('amazon') > -1) {
  // first check if in URL

  var id = "";
  var refst = here.indexOf('/ref=');
  if (refst > -1) {
    here = here.substring(43, refst);
    if (here.length > 1 && here.indexOf('/') == -1) {
      // found it!
      id = "list/" + here;
    }
  }
  if (id.length < 10) {
    // okay bugger, look in HTML
    //a[@class='blue-act']/@href
    
    result = document.evaluate("//a[@class='blue-act']", document, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null);
    for (var i =0; i<result.snapshotLength; i++) {
      node = result.snapshotItem(i);
      attr = node.getAttribute('href');
      
      var refst = attr.indexOf('/ref=');
      var wishst = attr.indexOf('/wishlist');
      if (refst > -1 && wishst > -1) {
	id = attr.substring(wishst+10, refst);
	id = "list/" + id;
      }
    }    
  }
  aggrURI = "http://foresite.cheshire3.org/amazon/ore/" + id;

 } else if (here.indexOf('myexperiment.org/packs') > -1) {
  pid = here.substring(here.indexOf('/packs/')+7, here.length);
  aggrURI = 'http://rdf.myexperiment.org/ResourceMap/Pack/' + pid;
  // broken content negotiation
  conneg_hdrs = {'Accept' : 'application/rdf+xml'}

 } else if (here.indexOf('groupme.org/GroupMe/group') > -1) {
  gid = here.substring(here.indexOf('/GroupMe/group/')+15, here.length);
  aggrURI = 'http://foresite.cheshire3.org/groupme/ore/group/' + gid;

 } else if (here.indexOf('groupme.org/GroupMe/tag') > -1) {
  gid = here.substring(here.indexOf('/GroupMe/tag/')+13, here.length);
  aggrURI = 'http://foresite.cheshire3.org/groupme/ore/tag/' + gid;

 } else if (here.indexOf('chroniclingamerica.loc.gov') > -1) {
  
  // look for link[@rel=resourcemap']
  result = document.evaluate("/html/head/link[@rel='resourcemap']", document, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null);
  for (var i =0; i<result.snapshotLength; i++) {
    node = result.snapshotItem(i);
    href = node.getAttribute('href');
    // href = href.replace('.rdf', '#issue')
    aggrURI = "http://chroniclingamerica.loc.gov" + href; 
  }

  after = document.getElementById('rss');
  if (after) {

    newDiv = document.createElement('div');

    a = document.createElement('a');
    a.setAttribute('href', aggrURI);  
    newDiv.appendChild(a);
    img = document.createElement('img');
    img.setAttribute('src', 'http://www.openarchives.org/ore/logos/ore_logo_14.png');
    a.appendChild(img);
  
    newDiv.appendChild(document.createTextNode('  '));

    a = document.createElement('a');
    a.setAttribute('href', 'javascript:none;');
    a.appendChild(document.createTextNode('ExplORE'));
    a.addEventListener('click', showSvg, true);
    newDiv.appendChild(a);
    after.appendChild(newDiv);
    done = 1;
  }
    
 } else if (remURI) {
  aggrURI = remURI;
 } else {
  return;
 }



if (!done) {
  newDiv = document.createElement('div');
  newDiv.setAttribute('class', 'foresite_popout');
  
  a = document.createElement('a');
  a.setAttribute('href', aggrURI);  
  newDiv.appendChild(a);
  img = document.createElement('img');
  img.setAttribute('src', 'http://www.openarchives.org/ore/logos/ore_logo_14.png');
  img.setAttribute('style', 'border: 0px solid white');
  a.appendChild(img);
  
  newDiv.appendChild(document.createTextNode('  '));

  span = document.createElement('span');
  span.setAttribute('style', 'position: absolute; top: 0px; left: 0px; width: 100%; text-align: right;');
  span.addEventListener('click', function(evt) {this.parentNode.style.zIndex = -1; this.parentNode.style.visibility='hidden';}, true);
  b = document.createElement('b');
  b.appendChild(document.createTextNode('[X]'));
  span.appendChild(b);
  newDiv.appendChild(span);
  
  a = document.createElement('a');
  a.setAttribute('href', 'javascript:none;');
  a.appendChild(document.createTextNode('ExplORE'));
  a.addEventListener('click', showSvg, true);
  newDiv.appendChild(a);
  
  body = document.getElementsByTagName('body')[0];
  body.appendChild(newDiv);
 }


// insert new style
style = document.createElement('style');
style.appendChild(document.createTextNode(styleText));
head = document.getElementsByTagName("head")[0];
head.appendChild(style);
  
top = document.getElementsByTagName('body');
top = top[0].childNodes[0];

cvs = createCanvas();
top.parentNode.insertBefore(cvs, top);

var bgNode = document.createElement('div');
bgNode.setAttribute('id', 'svgbackground');
bgNode.setAttribute('class', 'background');
top.parentNode.insertBefore(bgNode, top);

var spinNode = document.createElement('div');
spinNode.setAttribute('class', 'spinnerdiv');
spinNode.setAttribute('id', 'spinner');
spinImgNode = document.createElement('img');
spinImgNode.setAttribute('src', remoteHost + '/files/ajax-loader.gif');
spinImgNode.setAttribute('border', '0');
spinNode.appendChild(spinImgNode);
top.parentNode.insertBefore(spinNode, top);

init();
