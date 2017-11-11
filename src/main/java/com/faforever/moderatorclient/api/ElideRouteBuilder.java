package com.faforever.moderatorclient.api;

import com.github.jasminb.jsonapi.annotations.Type;
import com.github.rutledgepaulv.qbuilders.builders.QBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.visitors.RSQLVisitor;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class ElideRouteBuilder {
    private final Class<?> dtoClass;
    private String id = null;
    private Condition<?> filterCondition;
    private List<String> includes = new ArrayList<>();
    private Integer pageSize = null;
    private Integer pageNumber = null;

    public ElideRouteBuilder(Class<?> dtoClass) {
        this.dtoClass = dtoClass;
    }

    public static <T extends QBuilder<T>> QBuilder<T> qBuilder() {
        return new QBuilder<>();
    }

    public ElideRouteBuilder addInclude(String include) {
        includes.add(include);
        return this;
    }

    public ElideRouteBuilder id(String id) {
        Assert.state(filterCondition == null, "lookup of id and filter cannot be combined");
        Assert.state(pageSize == null, "lookup of id and pageSize cannot be combined");
        Assert.state(pageNumber == null, "lookup of id and pageNumber cannot be combined");
        this.id = id;
        return this;
    }

    public ElideRouteBuilder filter(Condition<?> eq) {
        Assert.state(id == null, "lookup of id and filter cannot be combined");
        filterCondition = eq;
        return this;
    }

    public ElideRouteBuilder pageSize(int size) {
        Assert.state(id == null, "lookup of id and filter cannot be combined");
        pageSize = size;
        return this;
    }

    public ElideRouteBuilder pageNumber(int number) {
        Assert.state(id == null, "lookup of id and filter cannot be combined");
        pageNumber = number;
        return this;
    }

    public String build() {
        String dtoPath = dtoClass.getDeclaredAnnotation(Type.class).value();

        StringJoiner queryArgs = new StringJoiner("&", "?", "")
                .setEmptyValue("");

        if (includes.size() > 0) {
            queryArgs.add(String.format("include=%s", includes.stream().collect(Collectors.joining(","))));
        }

        if (filterCondition != null) {
            queryArgs.add(String.format("filter=%s", filterCondition.query(new RSQLVisitor())));
        }

        if (pageSize != null) {
            queryArgs.add(String.format("page[size]=%s", pageSize));
        }

        if (pageNumber != null) {
            queryArgs.add(String.format("page[number]=%s", pageNumber));
        }

        return "/data/" +
                dtoPath +
                (id == null ? "" : "/" + id) +
                queryArgs.toString();
    }
}
