import { NextRequest } from 'next/server';
import { proxy } from '../../../../../lib/backend';

export const POST = (req: NextRequest) => proxy(req);
export const DELETE = (req: NextRequest) => proxy(req);
