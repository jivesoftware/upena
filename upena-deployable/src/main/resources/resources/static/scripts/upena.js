window.$ = window.jQuery;
window.upena = {};
upena.hs = {
    installed: null,
    queuedUninstall: null,
    init: function () {
        $('input[type=text].upena-hs-field').each(function (i, input) {
            var $inputName = $(input);
            var $inputKey = $(input).next('input[type=hidden]');
            var endpoint = $inputName.data('upenaLookup');
            var ab = $inputName.data('upenaRemote');

            $inputName.focus(function () {
                upena.hs.install($inputKey, $inputName, function (key, name) {
                    $inputKey.val(key);
                    $inputName.val(name);
                });
                upena.hs.lookup(endpoint, ab, $inputName.val());
            });
            $inputName.on('input', function () {
                $inputKey.val('');
                upena.hs.lookup(endpoint, ab, $inputName.val());
            });
            $inputName.blur(function () {
                upena.hs.queuedUninstall = setTimeout(function () {
                    upena.hs.uninstall($inputName);
                }, 200);
            });
        });
    },
    install: function ($inputKey, $inputName, callback) {
        upena.hs.cancelUninstall();
        upena.hs.uninstall();
        var $selector = upena.hs.makeSelector();
        $('body').append($selector);
        upena.hs.installed = {
            selector: $selector,
            inputKey: $inputKey,
            inputName: $inputName,
            callback: callback,
            ready: false
        };
        $inputName.removeClass('upena-hs-field-broken');
        var offset = $inputName.offset();
        var height = $inputName.height();
        $selector.show();
        $selector.offset({
            left: offset.left,
            top: offset.top + height + 10
        });
    },
    uninstall: function ($inputName) {
        if (!upena.hs.installed || $inputName && upena.hs.installed.inputName != $inputName) {
            return;
        }

        $inputName = $inputName || upena.hs.installed.inputName;
        var $inputKey = upena.hs.installed.inputKey;
        var name = $inputName.val();
        var found = false;
        var $selector = upena.hs.installed.selector;
        $selector.find('a').each(function (i) {
            var $a = $(this);
            if ($a.data('upenaName') == name) {
                var key = $a.data('upenaKey');
                $inputKey.val(key);
                found = true;
                return false;
            }
        });
        if (!found) {
            $inputName.addClass('upena-hs-field-broken');
            upena.hs.installed.inputKey.val('');
        }

        $selector.remove();
        upena.hs.installed = null;
    },
    makeSelector: function () {
        var $selector = $('<div>').addClass("upena-hs-selector");
        $selector.focus(function () {
            if (selector == upena.hs.installed.selector) {
                upena.hs.cancelUninstall();
            }
        });
        $selector.blur(function () {
            upena.hs.uninstall();
        });
        return $selector;
    },
    cancelUninstall: function () {
        if (upena.hs.queuedUninstall) {
            clearTimeout(upena.hs.queuedUninstall);
        }
    },
    picked: function (key, name) {
        if (upena.hs.installed) {
            upena.hs.installed.callback(key, name);
            upena.hs.uninstall();
        }
    },
    lookup: function (endpoint, ab, contains) {


        var host = "#" + ab + "RemoteHostPicker";
        var port = "#" + ab + "RemotePortPicker";

        console.log(host + " " + port);

        console.log($(host).attr('value') + " " + $(port).attr('value'));

        var $selector = upena.hs.installed.selector;
        $.ajax(endpoint, {
            data: {
                'contains': contains,
                'remoteHost': $(host).attr('value'),
                'remotePort': $(port).attr('value')
            }
        })
                .done(function (data) {
                    if (!upena.hs.installed || upena.hs.installed.selector != $selector) {
                        // selector changed during the query
                        return;
                    }
                    if (data.length) {
                        $selector.empty();
                        for (var i = 0; i < data.length; i++) {
                            $selector.append(
                                    "<a href='#'" +
                                    " class='upena-hs-choice'" +
                                    " data-upena-key='" + data[i].key + "'" +
                                    " data-upena-name='" + data[i].name + "'>" + data[i].name + "</a><br/>");
                        }
                        upena.hs.link($selector);
                        upena.hs.installed.ready = true;
                    } else {
                        $selector.html("<em>No matches</em>");
                    }
                });
    },
    link: function ($selector) {
        $selector.find('a').each(function (i) {
            var $a = $(this);
            var key = $a.data('upenaKey');
            var name = $a.data('upenaName');
            $a.click(function () {
                upena.hs.picked(key, name);
                return false;
            });
        });
    }
};
upena.instances = {
    init: function () {
    }
};


upena.clusterReleaseGroups = {
    add: function (clusterId) {
        var serviceId = $('#serviceId-' + clusterId).val();
        var releaseGroupId = $('#releaseId-' + clusterId).val();
        $.ajax("/ui/clusters/add", {
            data: JSON.stringify({'clusterId': clusterId, 'serviceId': serviceId, 'releaseGroupId': releaseGroupId}),
            method: "post",
            contentType: "application/json",
            success: function () {
                window.location.reload(true);
            },
            error: function () {
                alert('Save failed!');
            }
        });
    },
    remove: function (clusterId, serviceId, releaseGroupId) {
        $.ajax("/ui/clusters/remove", {
            data: JSON.stringify({'clusterId': clusterId, 'serviceId': serviceId, 'releaseGroupId': releaseGroupId}),
            method: "post",
            contentType: "application/json",
            success: function () {
                window.location.reload(true);
            },
            error: function () {
                alert('Save failed!');
            }
        });
    }
};
upena.instancePorts = {
    addPort: function (instanceId) {
        var portName = $('#portName-' + instanceId).val();
        var port = $('#port-' + instanceId).val();
        var propertyName = null;
        var propertyValue = null;
        console.log(portName + " " + port + " " + instanceId);
        $.ajax("/ui/instances/ports/add", {
            data: JSON.stringify({'instanceId': instanceId, 'portName': portName, 'port': port, 'propertyName': propertyName, 'propertyValue': propertyValue}),
            method: "post",
            contentType: "application/json",
            success: function () {
                window.location.reload(true);
            },
            error: function () {
                alert('Save failed!');
            }
        });
    },
    removePort: function (instanceId, portName) {
        var port = -1;
        var propertyName = null;
        var propertyValue = null;
        $.ajax("/ui/instances/ports/remove", {
            data: JSON.stringify({'instanceId': instanceId, 'portName': portName, 'port': port, 'propertyName': propertyName, 'propertyValue': propertyValue}),
            method: "post",
            contentType: "application/json",
            success: function () {
                window.location.reload(true);
            },
            error: function () {
                alert('Save failed!');
            }
        });
    },
    addPortProperty: function (instanceId, portName) {
        var port = -1;
        var propertyName = $('#portPropertyName-' + instanceId + '-' + portName).val();
        var propertyValue = $('#portPropertyValue-' + instanceId + '-' + portName).val();
        $.ajax("/ui/instances/ports/add", {
            data: JSON.stringify({'instanceId': instanceId, 'portName': portName, 'port': port, 'propertyName': propertyName, 'propertyValue': propertyValue}),
            method: "post",
            contentType: "application/json",
            success: function () {
                window.location.reload(true);
            },
            error: function () {
                alert('Save failed!');
            }
        });
    },
    removePortProperty: function (instanceId, portName, propertyName) {
        var port = -1;
        var propertyValue = null;
        $.ajax("/ui/instances/ports/remove", {
            data: JSON.stringify({'instanceId': instanceId, 'portName': portName, 'port': port, 'propertyName': propertyName, 'propertyValue': propertyValue}),
            method: "post",
            contentType: "application/json",
            success: function () {
                window.location.reload(true);
            },
            error: function () {
                alert('Save failed!');
            }
        });
    }
};
upena.cfg = {
    pending: {},
    save: function () {
        var updates = [];
        $.each(upena.cfg.pending, function (prop) {
            $.each(upena.cfg.pending[prop], function (instanceKey) {
                var value = upena.cfg.pending[prop][instanceKey] || "(default)";
                updates.push(instanceKey, ': ', prop, ' -> ', value, '\n');
            });
        });
        if (confirm(updates.join(''))) {
            $.ajax("/ui/config/modify", {
                data: JSON.stringify({'updates': upena.cfg.pending}),
                method: "post",
                contentType: "application/json",
                success: function () {
                    upena.cfg.pending = {};
                    upena.cfg.refreshPending();
                },
                error: function () {
                    alert('Save failed!');
                }
            });
        }
    },
    init: function () {
        $('#pending-save').click(upena.cfg.save);
        $('button.upena-cfg-link').each(function (i) {
            var $button = $(this);
            var link = $button.data('upenaCfgLink');
            var prop = $button.data('upenaCfgProp');
            $button.click(function () {
                var value = $('input[type=text][data-upena-cfg-link="' + link + '"]').val();
                $('input[type=text][data-upena-cfg-prop="' + prop + '"].upena-cfg-field-a').each(function () {
                    $(this).val(value);
                    upena.cfg.checkInput($(this));
                });
            });
        });
        $('button.upena-cfg-copy').each(function (i) {
            var $button = $(this);
            var copy = $button.data('upenaCfgCopy');
            var prop = $button.data('upenaCfgProp');
            $button.click(function () {
                var value = $('input[type=text][data-upena-cfg-copy="' + copy + '"]').val();
                $('input[type=text][data-upena-cfg-prop="' + prop + '"].upena-cfg-field-a').each(function () {
                    $(this).val(value);
                    upena.cfg.checkInput($(this));
                });
            });
        });
        $('input[type=text].upena-cfg-field-a').on('input', function () {
            upena.cfg.checkInput($(this));
        });
    },
    checkInput: function ($input) {
        var instanceKey = $input.data('upenaCfgInstanceKey');
        var prop = $input.data('upenaCfgProp');
        var override = $input.data('upenaCfgOverride');
        var value = $input.val();
        var modified = (override != value);
        if (!upena.cfg.pending[prop]) {
            upena.cfg.pending[prop] = {};
        }
        if (modified) {
            upena.cfg.pending[prop][instanceKey] = value;
        } else {
            delete upena.cfg.pending[prop][instanceKey];
        }
        upena.cfg.refreshPending();
    },
    refreshPending: function () {
        var $fixed = $('#upena-cfg-pending');
        var $pending = $('#pending-count');
        var changes = 0;
        $.each(upena.cfg.pending, function (prop) {
            $.each(upena.cfg.pending[prop], function (instanceKey) {
                changes++;
            });
        });
        $pending.text(changes);
        if (changes > 0) {
            $fixed.show();
        } else {
            $fixed.hide();
        }
    }
};

upena.query = {
    advanced: function (ele) {
        var $e = $(ele);
        if ($e.prop('checked')) {
            $('#query-filters').addClass('query-show-advanced');
        } else {
            $('#query-filters').removeClass('query-show-advanced');
        }
    }
};

upena.latestRelease = {
    change: function (inputId, value) {
        $('#' + inputId).val(value);
        return true;
    }
};


upena.build = {
    layouter: null,
    renderer: null,
    height: null,
    width: null,
    init: function () {

        upena.build.height = $(document).height() - 100;
        upena.build.width = $(document).width() - 100;
        var nodes = $('#upena-build').data('nodes');
        var edges = $('#upena-build').data('edges');
        /* http://www.graphdracula.net/ */
        var g = new Graph();
        $(nodes).each(function (key, node) {
            var render = function (r, n) {


                /* the Raphael set is obligatory, containing all you want to display */
                var iconSize = parseInt(node.iconSize);
                var halfIconSize = iconSize / 2;

                var icon;
                var text;
                var bb;

                var cx = n.point[0];
                var cy = n.point[1];

                if (node.label) {
                    text = r.text(cx, cy + iconSize, node.label).attr({"font-size": node.fontSize + "px", opacity: 1.0, fill: "#000"});
                    bb = text.getBBox(false);
                }

                if (node.icon) {
                    icon = r.image("/static/img/" + node.icon + ".png", cx - halfIconSize, cy - halfIconSize, iconSize, iconSize);
                    bb = {x: cx - halfIconSize, y: cy - halfIconSize, x2: cx + iconSize, y2: cy + iconSize, width: iconSize, height: iconSize};
                }

                var w = bb.width;
                var h = bb.height;
                var s = (Math.max(w, h) / 2) + 2;

                var rectbg = r.circle(cx, cy - 2, s).attr({
                    stroke: "none",
                    fill: "#fff",
                    "stroke-width": "1px",
                    opacity: 1,
                });
                var rectfg = r.circle(cx, cy - 2, s).attr({
                    stroke: "#000",
                    fill: "#" + node.color,
                    "stroke-width": "1px",
                    opacity: 1,
                });

                var set = r.set();
                set.push(rectbg);
                set.push(rectfg);
                if (icon) {
                    set.push(icon);
                    icon.toFront();
                }
                if (text) {
                    set.push(text);
                    text.toFront();
                }

                if (node.tooltip) {

                    set.items.forEach(function (el) {

                        var tts = r.set();
                        var text = r.text(-70, -100, node.tooltip).attr({"font-size": "16px", opacity: 1.0, fill: "#000"});
                        var bb = text.getBBox(true);
                        bb.width += 10;
                        bb.height += 10;

                        var ttr = r.rect(-70 - (bb.width / 2), -100 - (bb.height / 2), bb.width, bb.height).attr({"fill": "#999", "stroke-width": 1, r: "4px"});
                        var tt = tts.push(ttr).push(text);
                        text.toFront();
                        el.tooltip(tt);
                    });
                }
                return set;
            };
            var clicked = function () {
                $("#build-details").html(node.focusHtml);
            };
            g.addNode(node.id, {label: node.label, render: render, clicked: clicked});
        });
        $(edges).each(function (key, edge) {

            g.addEdge(edge.from, edge.to, {
                label: edge.label,
                straight: false,
                directed: true,
                stroke: "#" + edge.color,
                fill: "#" + edge.color
            });
        });
        /* layout the graph using the Spring layout implementation */
        upena.build.layouter = new Graph.Layout.Spring(g);
        upena.build.layouter.layout();
        /* draw the graph using the RaphaelJS draw implementation */
        upena.build.renderer = new Graph.Renderer.Raphael('upena-build', g, upena.build.width, upena.build.height);

        upena.build.renderer.draw();


    },
    redraw: function () {
        dracula_graph_seed = 1;
        upena.topology.layouter.layout();
        upena.topology.renderer.draw();
    }
};


upena.topology = {
    layouter: null,
    renderer: null,
    height: null,
    width: null,
    init: function () {

        upena.topology.height = ($(document).height() / 4) * 3;
        upena.topology.width = $(document).width() - 100;
        var nodes = $('#upena-topology').data('nodes');
        var edges = $('#upena-topology').data('edges');
        var legend = $('#upena-topology').data('legend');
        /* http://www.graphdracula.net/ */
        var g = new Graph();
        $(nodes).each(function (key, node) {
            var render = function (r, n) {


                /* the Raphael set is obligatory, containing all you want to display */
                var hs = 10;
                var iconSize = 24;
                var halfIconSize = iconSize / 2;

                var icon;
                var text;
                var bb;

                var cx = n.point[0];
                var cy = n.point[1];

                if (node.label) {
                    text = r.text(cx, cy + iconSize, node.label).attr({"font-size": node.fontSize + "px", opacity: 1.0, fill: "#000"});
                    bb = text.getBBox(false);
                }

                if (node.icon) {
                    icon = r.image("/static/img/" + node.icon + ".png", cx - halfIconSize, cy - halfIconSize, iconSize, iconSize);
                    bb = {x: cx - halfIconSize, y: cy - halfIconSize, x2: cx + iconSize, y2: cy + iconSize, width: iconSize, height: iconSize};
                }

                var w = bb.width;
                var h = bb.height;
                var s = (Math.max(w, h) / 2) + 2;

                var rectbg = r.circle(cx, cy - 2, s).attr({
                    stroke: "none",
                    fill: "#fff",
                    "stroke-width": "1px",
                    opacity: 1,
                });
                var rectfg = r.circle(cx, cy - 2, s).attr({
                    stroke: "#000",
                    fill: "#" + node.color,
                    "stroke-width": "1px",
                    opacity: 0.4,
                });

                var health = r.circle(cx, cy - 2, parseInt(s) + parseInt(node.healthRadius)).attr({
                    stroke: "#111",
                    fill: "270-#" + node.maxbgcolor + "-#" + node.minbgcolor,
                    "stroke-width": "1px",
                    opacity: 1.0,
                });

                health.toBack();

                var set = r.set();
                set.push(rectbg);
                set.push(rectfg);
                set.push(health);
                if (icon) {
                    set.push(icon);
                    icon.toFront();
                }
                if (text) {
                    set.push(text);
                    text.toFront();
                }

                if (node.tooltip) {

                    set.items.forEach(function (el) {

                        var tts = r.set();
                        var text = r.text(-70, -100, node.tooltip).attr({"font-size": "16px", opacity: 1.0, fill: "#000"});
                        var bb = text.getBBox(true);
                        bb.width += 10;
                        bb.height += 10;

                        var ttr = r.rect(-70 - (bb.width / 2), -100 - (bb.height / 2), bb.width, bb.height).attr({"fill": "#999", "stroke-width": 1, r: "4px"});
                        var tt = tts.push(ttr).push(text);
                        text.toFront();
                        el.tooltip(tt);
                    });
                }
                return set;
            };
            var clicked = function () {
                $("#topology-health").html(node.focusHtml);
            };
            g.addNode(node.id, {label: node.label, render: render, clicked: clicked});
        });
        $(edges).each(function (key, edge) {

            g.addEdge(edge.from, edge.to, {
                label: edge.label,
                straight: true,
                directed: false,
                stroke: "#" + edge.minColor,
                fill: "#" + edge.maxColor
            });
        });
        /* layout the graph using the Spring layout implementation */
        upena.topology.layouter = new Graph.Layout.Spring(g);
        upena.topology.layouter.layout();
        /* draw the graph using the RaphaelJS draw implementation */
        upena.topology.renderer = new Graph.Renderer.Raphael('upena-topology', g, upena.topology.width, upena.topology.height);

        upena.topology.renderer.draw();


        var r = upena.topology.renderer.r;

        if (legend) {
            var x = 48;
            var y = 10;
            for (i = 0; i < legend.length; i++) {

                var text = r.text(x, y, legend[i].name);
                text.attr({"font-size": 16 + "px", opacity: 1.0, fill: "#000"});
                var bb = text.getBBox(true);
                var rect = r.rect(x - ((bb.height * 2)), y - (bb.height / 2), bb.height, bb.height);
                text.attr({x: bb.x + bb.width});
                rect.attr({
                    stroke: "#000",
                    fill: "#" + legend[i].color,
                    r: "6px",
                    "stroke-width": "1px",
                    opacity: 0.4,
                });

                y += bb.height + 5;
            }
        }
    },
    redraw: function () {
        dracula_graph_seed = 1;
        upena.topology.layouter.layout();
        upena.topology.renderer.draw();
    }
};

upena.connectivity = {
    layouter: null,
    renderer: null,
    height: null,
    width: null,
    init: function () {

        upena.connectivity.height = ($(document).height() / 4) * 3;
        upena.connectivity.width = $(document).width() - 100;
        var nodes = $('#upena-connectivity').data('nodes');
        var edges = $('#upena-connectivity').data('edges');
        var legend = $('#upena-connectivity').data('legend');
        /* http://www.graphdracula.net/ */
        var g = new Graph();
        $(nodes).each(function (key, node) {
            var render = function (r, n) {


                /* the Raphael set is obligatory, containing all you want to display */
                var pad = 12;
                var hs = 10;
                var text = r.text(n.point[0], n.point[1], n.label).attr({"font-size": node.fontSize + "px", opacity: 1.0, fill: "#000"});
                var bb = text.getBBox(true);
                var w = hs + (pad / 2) + bb.width + pad;
                var h = bb.height + pad;
                var rect = r.rect(n.point[0] - (w / 2) - (hs / 2), n.point[1] - (h / 2), w, h).attr({
                    stroke: "#000",
                    fill: "#" + node.color,
                    r: "12px",
                    "stroke-width": "1px",
                    opacity: 0.4,
                });
                var health = r.rect(n.point[0] - (w / 2) - (hs / 2) + (pad / 2), n.point[1] - (h / 2) + (pad / 2), hs, hs).attr({
                    stroke: "#111",
                    fill: "270-#" + node.maxbgcolor + "-#" + node.minbgcolor,
                    r: "4px",
                    "stroke-width": "1px",
                    opacity: 1.0,
                });

                var set = r.set();
                set.push(rect);
                set.push(health);
                set.push(text);

                //set.items.forEach(function(el) {el.tooltip(r.set().push(r.rect(-70,-100, 30, 30).attr({"fill": "#999", "stroke-width": 1, r : "4px"})))});

                text.toFront();
                return set;
            };
            var clicked = function () {
                $("#connectivity-health").html(node.focusHtml);
            };
            g.addNode(node.id, {label: node.label, render: render, clicked: clicked});
        });
        $(edges).each(function (key, edge) {

            g.addEdge(edge.from, edge.to, {
                label: edge.label,
                directed: true,
                stroke: "#" + edge.minColor,
                fill: "#" + edge.maxColor
            });
        });
        /* layout the graph using the Spring layout implementation */
        upena.connectivity.layouter = new Graph.Layout.Spring(g);
        upena.connectivity.layouter.layout();
        /* draw the graph using the RaphaelJS draw implementation */
        upena.connectivity.renderer = new Graph.Renderer.Raphael('upena-connectivity', g, upena.connectivity.width, upena.connectivity.height);
        upena.connectivity.renderer.draw();

        var r = upena.connectivity.renderer.r;
        if (legend) {
            var x = 48;
            var y = 10;
            for (i = 0; i < legend.length; i++) {

                var text = r.text(x, y, legend[i].name);
                text.attr({"font-size": 16 + "px", opacity: 1.0, fill: "#000"});
                var bb = text.getBBox(true);
                var rect = r.rect(x - ((bb.height * 2)), y - (bb.height / 2), bb.height, bb.height);
                text.attr({x: bb.x + bb.width});
                rect.attr({
                    stroke: "#000",
                    fill: "#" + legend[i].color,
                    r: "6px",
                    "stroke-width": "1px",
                    opacity: 0.4,
                });

                y += bb.height + 5;
            }
        }
    },
    redraw: function () {
        dracula_graph_seed = 1;
        upena.connectivity.layouter.layout();
        upena.connectivity.renderer.draw();
    }
};

upena.health = {
    color: {},
    text: {},
    age: {},
    init: function () {
        setTimeout(upena.health.poll, 1000);
    },
    poll: function () {
        if (upena.windowFocused) {
            $.ajax("/ui/health/live", {
                method: "get",
                data: {},
                contentType: "application/json",
                success: function (data) {
                    upena.health.redraw(data);
                    setTimeout(upena.health.poll, 1000);
                },
                error: function () {
                    setTimeout(upena.health.poll, 5000);
                }
            });
        } else {
            setTimeout(upena.health.poll, 1000);
        }
    },
    redraw: function (data) {
        for (var i = 0; i < data.length; i++) {
            var id = data[i].id;
            if (!upena.health.color[id]) {
                var $cell = $('[data-health-hook="' + id + '"');
                upena.health.color[id] = $cell.find('.health-color');
                upena.health.text[id] = $cell.find('.health-text');
                upena.health.age[id] = $cell.find('.health-age');
            }
            upena.health.color[id].css('background-color', "rgb(" + data[i].color + ")");
            upena.health.text[id].html(data[i].text || '&nbsp;&nbsp;&nbsp;');
            upena.health.age[id].html(data[i].age);
        }
    }
};


upena.sar = {
    waves: {},
    data: {},
    initChart: function (which) {
        var $canvas = $(which);
        var ctx = which.getContext("2d");
        var id = $canvas.data('sarWaveId');
        if (!upena.sar.waves[id]) {
            var type = "Line"; //$canvas.data('sarWaveType');
            var data = upena.sar.data[id];
            upena.sar.waves[id] = (new Chart(ctx))[type](data, {
                multiTooltipTemplate: "<%= datasetLabel %> - <%= value %>",
                scaleLineColor: "rgba(128,128,128,0.5)",
                tooltipFillColor: "rgba(0,0,0,1)",
                pointDot: false,
                bezierCurve: false,
                datasetFill: false,
                responsive: true,
                animation: false
            });
        }
        upena.sar.waves[id].update();
    },
    init: function () {
        
        $('.sar-wave').each(function (i) {
            upena.sar.initChart(this);
        });
    }
};


$(document).ready(function () {


    upena.windowFocused = true;
    upena.onWindowFocus = [];
    upena.onWindowBlur = [];

    Ladda.bind('.ladda-button', {timeout: 60000});

    if ($('.upena-hs-field').length) {
        upena.hs.init();
    }
    if ($('#upena-instances').length) {
        upena.instances.init();
    }
    if ($('#upena-cfg').length) {
        upena.cfg.init();
    }
    if ($('#upena-build').length) {
        upena.build.init();
    }
    if ($('#upena-topology').length) {
        upena.topology.init();
    }
    if ($('#upena-connectivity').length) {
        upena.connectivity.init();
    }
    if ($('#upena-health').length) {
        upena.health.init();
    }

    (function () {
        var hack = {};
        $('[rel="popover"]').popover({
            container: 'body',
            html: true,
            content: function () {
                console.log($(this).attr('id'));
                var h = $($(this).data('popover-content')).removeClass('hide');
                hack[$(this).attr('id')] = h;
                return h;
            }
        }).click(function (e) {
            e.preventDefault();
        }).on('hidden.bs.popover', function () {
            var h = hack[$(this).attr('id')];
            if (h) {
                h.detach();
                h.addClass('hide');
                $('body').append(h);
            }
        });
    })();

    (function () {
        var hack = {};
        $('div.popover-health').popover({
            container: 'body',
            html: true,
            content: function () {
                console.log($(this).attr('id'));
                var h = $($(this).data('popover-content')).removeClass('hide');
                hack[$(this).attr('id')] = h;
                return h;
            }
        }).click(function (e) {
            e.preventDefault();
        }).on('show.bs.popover', function () {
            var instanceKey = $(this).data('popoverInstanceKey');
            if (instanceKey) {
                var h = hack[$(this).attr('id')];
                $.ajax("/ui/health/uis", {
                    method: "get",
                    data: {"instanceKey": instanceKey},
                    success: function (data) {
                        $(h).find(".uis").html(data);
                    },
                    error: function () {
                        $(h).find(".uis").html("Failed to load UIs");
                    }
                });
            }

        }).on('hidden.bs.popover', function () {
            var h = hack[$(this).attr('id')];
            if (h) {
                h.detach();
                h.addClass('hide');
                $('body').append(h);
            }
        });
    })();


    if ($('#sar-waveform').length) {
        upena.sar.init();
    }

});

$(window).focus(function () {
    upena.windowFocused = true;
    for (var i = 0; i < upena.onWindowFocus.length; i++) {
        upena.onWindowFocus[i]();
    }
}).blur(function () {
    upena.windowFocused = false;
    for (var i = 0; i < upena.onWindowBlur.length; i++) {
        upena.onWindowBlur[i]();
    }
});
