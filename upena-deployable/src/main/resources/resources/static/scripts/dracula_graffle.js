/**
 * Originally grabbed from the official RaphaelJS Documentation
 * http://raphaeljs.com/graffle.html
 * Adopted (arrows) and commented by Philipp Strathausen http://blog.ameisenbar.de
 * Licenced under the MIT licence.
 */

/**
 * Usage:
 * connect two shapes
 * parameters: 
 *      source shape [or connection for redrawing], 
 *      target shape,
 *      style with { fg : linecolor, bg : background color, directed: boolean }
 * returns:
 *      connection { draw = function() }
 */
Raphael.fn.connection = function (obj1, obj2, style) {
    var selfRef = this;
    /* create and return new connection */
    var edge = {/*
     from : obj1,
     to : obj2,
     style : style,*/
        draw: function () {
            /* get bounding boxes of target and source */
            var bb1 = obj1.getBBox();
            var bb2 = obj2.getBBox();
            var off1 = 0;
            var off2 = 0;

            if (style.straight) {
                bb1.x += bb1.width/2;
                bb2.x += bb2.width/2;
                bb1.y += bb1.height/2;
                bb2.y += bb2.height/2;
                bb1.width = 2;
                bb2.width = 2;
                bb1.height = 2;
                bb2.height = 2;
            } else {
                bb1.x -= 4;
                bb2.x -= 4;
                bb1.y -= 4;
                bb2.y -= 4;
                bb1.width += 8;
                bb2.width += 8;
                bb1.height += 8;
                bb2.height += 8;
            }


            /* coordinates for potential connection coordinates from/to the objects */
            var p = [
                {x: bb1.x + bb1.width / 2, y: bb1.y - off1}, /* NORTH 1 */
                {x: bb1.x + bb1.width / 2, y: bb1.y + bb1.height + off1}, /* SOUTH 1 */
                {x: bb1.x - off1, y: bb1.y + bb1.height / 2}, /* WEST  1 */
                {x: bb1.x + bb1.width + off1, y: bb1.y + bb1.height / 2}, /* EAST  1 */
                {x: bb2.x + bb2.width / 2, y: bb2.y - off2}, /* NORTH 2 */
                {x: bb2.x + bb2.width / 2, y: bb2.y + bb2.height + off2}, /* SOUTH 2 */
                {x: bb2.x - off2, y: bb2.y + bb2.height / 2}, /* WEST  2 */
                {x: bb2.x + bb2.width + off2, y: bb2.y + bb2.height / 2}  /* EAST  2 */
            ];

            /* distances between objects and according coordinates connection */
            var d = {}, dis = [];

            /*
             * find out the best connection coordinates by trying all possible ways
             */
            /* loop the first object's connection coordinates */
            for (var i = 0; i < 4; i++) {
                /* loop the seond object's connection coordinates */
                for (var j = 4; j < 8; j++) {
                    var dx = Math.abs(p[i].x - p[j].x),
                            dy = Math.abs(p[i].y - p[j].y);
                    if ((i == j - 4) || (((i != 3 && j != 6) || p[i].x < p[j].x) && ((i != 2 && j != 7) || p[i].x > p[j].x) && ((i != 0 && j != 5) || p[i].y > p[j].y) && ((i != 1 && j != 4) || p[i].y < p[j].y))) {
                        dis.push(dx + dy);
                        d[dis[dis.length - 1].toFixed(3)] = [i, j];
                    }
                }
            }
            var res = dis.length == 0 ? [0, 4] : d[Math.min.apply(Math, dis).toFixed(3)];
            /* bezier path */
            var x1 = p[res[0]].x,
                    y1 = p[res[0]].y,
                    x4 = p[res[1]].x,
                    y4 = p[res[1]].y,
                    dx = Math.max(Math.abs(x1 - x4) / 2, 10),
                    dy = Math.max(Math.abs(y1 - y4) / 2, 10),
                    x2 = [x1, x1, x1 - dx, x1 + dx][res[0]].toFixed(3),
                    y2 = [y1 - dy, y1 + dy, y1, y1][res[0]].toFixed(3),
                    x3 = [0, 0, 0, 0, x4, x4, x4 - dx, x4 + dx][res[1]].toFixed(3),
                    y3 = [0, 0, 0, 0, y1 + dy, y1 - dy, y4, y4][res[1]].toFixed(3);
            /* assemble path and arrow */
            var path = null;
            if (style && style.straight) {
                path = ["M", bb1.x, bb1.y, "L", bb2.x, bb2.y].join(",");
            } else {
                path = ["M", x1.toFixed(3), y1.toFixed(3), "C", x2, y2, x3, y3, x4.toFixed(3), y4.toFixed(3)].join(",");
            }
            /* arrow */
            if (style && style.directed) {
                /* magnitude, length of the last path vector */
                var mag = Math.sqrt((y4 - y3) * (y4 - y3) + (x4 - x3) * (x4 - x3));
                /* vector normalisation to specified length  */
                var norm = function (x, l) {
                    return (-x * (l || 9) / mag);
                };
                /* calculate array coordinates (two lines orthogonal to the path vector) */
                var arr = [
                    {x: (norm(x4 - x3) + norm(y4 - y3) + x4).toFixed(3), y: (norm(y4 - y3) + norm(x4 - x3) + y4).toFixed(3)},
                    {x: (norm(x4 - x3) - norm(y4 - y3) + x4).toFixed(3), y: (norm(y4 - y3) - norm(x4 - x3) + y4).toFixed(3)}
                ];
                path = path + ",M" + arr[0].x + "," + arr[0].y + ",L" + x4 + "," + y4 + ",L" + arr[1].x + "," + arr[1].y;
            }

            /* applying path(s) */

            if (edge.fg) {
                edge.fg.attr({path: path});
            } else {
                edge.fg = selfRef.path(path).attr({
                    stroke: style && style.fill.split("|")[0],
                        fill: "none",
                        "stroke-width": 2,
                        "stroke-dasharray": [". "]
                    }).toBack();
                }

            if (edge.bg) {
                edge.bg.attr({path: path});
            } else {
                if (style && style.fill && style.fill.split) {
                    var path = selfRef.path(path).attr({
                        stroke: style.stroke || "#000",
                        fill: "none",
                        "stroke-width": 1
                    }).toBack();

                    edge.bg = path;
                }
            }


            /* setting label */
            if (style && style.label) {
                var x = (x1 + x4) / 2;
                var y = (y1 + y4) / 2;
                if (edge.label) {
                    console.log(edge.label);
                    var bb = edge.label[0].getBBox(true);
                    var rh = bb.height + 8;
                    var rw = bb.width + 8;

                    edge.label[0].attr({x: x, y: y});
                    edge.label[1].attr({x: x - (rw / 2), y: y - (rh / 2)});
                } else {

                    var text = selfRef.text(x, y, style.label).attr({
                        stroke: "none",
                        fill: "#000",
                        "font-size": "10px"});

                    var bb = text.getBBox(true);
                    var rh = bb.height + 8;
                    var rw = bb.width + 8;
                    var rect = selfRef.rect(x - (rw / 2), y - (rh / 2), rw, rh).attr({
                        stroke: "#999",
                        fill: "#fff",
                        r: "6px",
                        opacity: 1.0
                    });


                    text.toFront();

                    var set = selfRef.set();
                    set.push(text);
                    set.push(rect);

                    //set.items.forEach(function(el) {el.tooltip(selfRef.set().push(selfRef.rect(-70,-100, 30, 30).attr({"fill": "#999", "stroke-width": 1, r : "4px"})))});

                    edge.label = set;
                }
            }
        }
    }
    edge.draw();
    return edge;
};
