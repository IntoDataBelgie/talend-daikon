// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.daikon.avro.visitor.record;

import java.nio.ByteBuffer;

import org.talend.daikon.avro.visitor.path.TraversalPath;

/**
 * Wrapper implementation for Bytes Avro type
 */
public class VisitableBytes extends AbstractVisitableStructure<ByteBuffer> {

    VisitableBytes(ByteBuffer value, TraversalPath path) {
        super(value, path);
    }

    @Override
    public void accept(RecordVisitor visitor) {
        visitor.visit(this);

    }
}
