import { NextRequest } from 'next/server';
import { proxy } from '../../../../lib/backend';

export const DELETE = (req: NextRequest) => proxy(req);
