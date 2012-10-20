package com.facebook.swift.generator.visitors;

import com.facebook.swift.parser.model.Service;
import com.facebook.swift.parser.visitor.DocumentVisitor;
import com.facebook.swift.parser.visitor.Visitable;
import com.google.common.collect.Lists;

import java.util.List;

public class ServiceVisitor implements DocumentVisitor
{
    private final List<Service> services = Lists.newArrayList();

    @Override
    public boolean accept(final Visitable visitable)
    {
        return visitable.getClass() == Service.class;
    }

    @Override
    public void visit(final Visitable visitable)
    {
        final Service service = Service.class.cast(visitable);
        services.add(service);
    }

    @Override
    public String getName()
    {
        return "service";
    }

    public List<Service> getServices()
    {
        return services;
    }
}
