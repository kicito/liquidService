
/*
 * Copyright (C) 2019-2021 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com>
 * Copyright (C) 2024 Narongrit Unwerawattana <narongrit.kie@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package joliex.liquidservice;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jolie.js.JsUtils;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.embedding.RequestResponse;
import liqp.Template;
import liqp.filters.Filter;

public class LiquidService extends JavaService {

    static final HashMap< String, Template> templatesMap = new HashMap<>();

    static {
        Filter.registerFilter(new Filter("useTemplate") {
            @Override
            public Object apply(Object value, Object... params) {
                ObjectMapper objectMapper = new ObjectMapper();
                String input = "";
                try {
                    input = objectMapper.writeValueAsString(value);
                } catch (JsonProcessingException ex) {
                    Logger.getLogger(LiquidService.class.getName()).log(Level.SEVERE, null, ex);
                }
                String templateName = super.asString(params[0]);
                if (templatesMap.containsKey(templateName)) {
                    return templatesMap.get(templateName).render(input);
                } else {
                    return "";
                }
            }
        });
    }

    @RequestResponse
    public void loadTemplate(Value request) {
        templatesMap.put(
                request.getFirstChild("name").strValue(),
                Template.parse(request.getFirstChild("template").strValue())
        );
    }

    @RequestResponse
    public Value renderDocument(Value request) throws FaultException {
        String format = request.getFirstChild("format").strValue();
        String data = null;
        if (format.equalsIgnoreCase("jolie")) {
            StringBuilder sb = new StringBuilder();
            try {
                JsUtils.valueToJsonString(request.getFirstChild("data"), false, null, sb);
                data = sb.toString();
            } catch (IOException e) {
                throw new FaultException("IOException", e.getMessage());
            }
        }
        if (format.equalsIgnoreCase("json")) {
            data = request.getFirstChild("data").strValue();
        }
        if (data != null) {
            Template template = Template.parse(request.getFirstChild("template").strValue());
            String rendering = template.render(data);
            return Value.create(rendering);
        } else {
            throw new FaultException("IOException", "Currently only the Jolie or JSON data is supported, provided: " + format);
        }
    }

}
