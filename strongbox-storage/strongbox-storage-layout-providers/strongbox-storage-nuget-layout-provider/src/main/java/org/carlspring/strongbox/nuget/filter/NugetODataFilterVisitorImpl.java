package org.carlspring.strongbox.nuget.filter;

import javax.persistence.criteria.Predicate.BooleanOperator;

import org.apache.commons.lang3.StringUtils;
import org.carlspring.strongbox.artifact.ArtifactTag;
import org.carlspring.strongbox.artifact.criteria.ArtifactEntryCriteria;
import org.carlspring.strongbox.data.criteria.Expression.ExpOperator;
import org.carlspring.strongbox.data.criteria.Predicate;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.FilterContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.FilterExpContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.FuctionExpContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.TokenExpContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.TokenExpFunctionContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.TokenExpLeftContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.TokenExpRightContext;

/**
 * @author sbespalov
 *
 */
public class NugetODataFilterVisitorImpl extends NugetODataFilterBaseVisitor<Predicate>
{

    private Predicate root;

    public NugetODataFilterVisitorImpl(Predicate p)
    {
        super();
        this.root = p;
    }

    @Override
    public Predicate visitFilter(FilterContext ctx)
    {
        Predicate p = super.visitFilter(ctx);
        if (p != root)
        {
            root.and(p);
        }
        return root;
    }

    @Override
    public Predicate visitFilterExp(FilterExpContext ctx)
    {
        if (ctx.tokenExp() != null)
        {
            return visitTokenExp(ctx.tokenExp());
        }
        else if (ctx.vNesteedFilterExp != null)
        {
            return visitFilterExp(ctx.vNesteedFilterExp);
        }

        BooleanOperator booleanOperator = BooleanOperator.valueOf(ctx.vLogicalOp.getText().toUpperCase());

        Predicate p1 = visitFilterExp(ctx.vFilterExpLeft);
        Predicate p2 = visitFilterExp(ctx.vFilterExpRight);

        if (BooleanOperator.AND.equals(booleanOperator))
        {
            root.and(p1).and(p2);
        }
        else if (BooleanOperator.OR.equals(booleanOperator))
        {
            root.and(p1.or(p2));
        }

        return root;
    }

    @Override
    public Predicate visitTokenExp(TokenExpContext ctx)
    {
        if (ctx.TAG() != null)
        {
            ArtifactEntryCriteria c = new ArtifactEntryCriteria();
            c.getTagSet().add(ArtifactTag.LAST_VERSION);

            return Predicate.of(ExpOperator.CONTAINS.of("tagSet.name", ArtifactTag.LAST_VERSION));
        }

        Predicate p = visitTokenExpLeft(ctx.vTokenExpLeft);

        String attributeValue = ctx.vTokenExpRight.getText();
        attributeValue = StringUtils.unwrap(attributeValue, "'");
        p.getExpression().setValue(attributeValue);

        return p;
    }

    @Override
    public Predicate visitTokenExpRight(TokenExpRightContext ctx)
    {
        return super.visitTokenExpRight(ctx);
    }

    @Override
    public Predicate visitTokenExpLeft(TokenExpLeftContext ctx)
    {
        if (ctx.ATTRIBUTE() != null)
        {
            String attribute = ctx.ATTRIBUTE().getText();
            return Predicate.of(ExpOperator.EQ.of(String.format("artifactCoordinates.coordinates.%s",
                                                                attribute),
                                                  null));
        }
        return visitTokenExpFunction(ctx.tokenExpFunction());
    }

    @Override
    public Predicate visitTokenExpFunction(TokenExpFunctionContext ctx)
    {
        String attribute = ctx.ATTRIBUTE().getText();

        if (ctx.fuctionExp().TO_LOWER() != null) {
            attribute = String.format("%s.toLowerCase()", attribute);
        }

        return Predicate.of(ExpOperator.EQ.of(String.format("artifactCoordinates.coordinates.%s",
                                                            attribute),
                                              null));
    }

}
